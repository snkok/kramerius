package cz.incad.Kramerius.views.localprint;

import java.awt.Dimension;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.xpath.XPathExpressionException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import cz.incad.Kramerius.Initializable;
import cz.incad.kramerius.FedoraAccess;
import cz.incad.kramerius.ObjectPidsPath;
import cz.incad.kramerius.SolrAccess;
import cz.incad.kramerius.security.IsActionAllowed;
import cz.incad.kramerius.security.SecuredActions;
import cz.incad.kramerius.security.User;
import cz.incad.kramerius.utils.ApplicationURL;
import cz.incad.kramerius.utils.StringUtils;
import cz.incad.kramerius.utils.conf.KConfiguration;
import cz.incad.kramerius.utils.imgs.KrameriusImageSupport;

public class PicturePrepareViewObject extends AbstractPrepareViewObject  implements Initializable{

    @Inject
    Provider<HttpServletRequest> servletRequestProvider;
    
    @Inject
    @Named("securedFedoraAccess")
    FedoraAccess fedoraAccess;

    @Inject
    Provider<User> userProvider;

    @Inject
    IsActionAllowed actionAllowed;
    
    @Inject
    SolrAccess solrAccess;
    
    private List<String> pids = new ArrayList<String>();
    
    
    @Override
    public void init() {
        try {
            double ratio = KConfiguration.getInstance().getConfiguration().getDouble("search.print.pageratio",1.414);
            HttpServletRequest request = this.servletRequestProvider.get();
            String pidsString = request.getParameter("pids");
            String[] pids = pidsString.split(",");
            String transcode = request.getParameter("transcode");
            int bits = numberOfBits(pids.length);
            for (int i = 0; i < pids.length; i++) {
                String p = pids[i];
                boolean canBeRead = canBeRead(p);
                if (canBeRead) {
                    p = this.fedoraAccess.findFirstViewablePid(p);
                    String ident = createIdent(i,bits); 
                    this.pids.add(URLDecoder.decode(p, "UTF-8"));

                    String url ="../img?pid="+URLEncoder.encode(p,"UTF-8")+"&stream=IMG_FULL&action="+(Boolean.parseBoolean(transcode) ? "TRANSCODE":"GETRAW");
                    String imageElement = "<img src='"+url+"' id='"+ident+"'></img>";
                    this.imgelements.add(imageElement);
                    
                    Dimension readDim = KrameriusImageSupport.readDimension(p, "IMG_FULL", fedoraAccess, 0);
                    createStyle(ratio, ident, readDim);
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean canBeRead(String pid) throws IOException {
        ObjectPidsPath[] paths = solrAccess.getPath(pid);
        for (ObjectPidsPath pth : paths) {
            if (this.actionAllowed.isActionAllowed(userProvider.get(), SecuredActions.READ.getFormalName(), pid, null, pth)) {
                return true;
            }
        }
        return false;
    }

}
