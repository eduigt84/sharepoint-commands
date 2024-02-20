package sharepoint.igt.com.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import sharepoint.beans.igt.com.User;

import static sharepoint.igt.com.utils.SharepointUtils.entityDataToUser;

public class SharepointContext extends SharepointHttpContext {
    private static final int MIN_USERS_QUERY_LENGTH = 3;
    private static final Logger LOG = LoggerFactory.getLogger(SharepointContext.class);

    public SharepointContext(String clientID, String secretID, String domain, String site) {
        super(clientID, secretID, domain, site);
    }

    public JSONObject getAllSiteLists() {
        LOG.debug("Getting all lists for site: {}", this.getSharepointAuth().getSite());
        HttpGet getRequest = generateRequest(HttpGet.class, "_api/lists");
        JSONObject json = executeQuery(getRequest);

        return json;
    }

    public JSONObject getListItemsByTitle(String title) {
        LOG.debug("Getting list items by title... {}", title);
        HttpGet getRequest = generateRequest(HttpGet.class, String.format("_api/web/lists/getbytitle('%s')/Items", title));
        JSONObject json = executeQuery(getRequest);

        return json;
    }

    public JSONArray getPagesListByRelativePath(String targetRelativeFolder) {
        LOG.debug("Pages List ... targetRelativeFolder:{}", targetRelativeFolder);
        String encodedTargetFolder = encode("Site Pages");
        String encodedW = encode("startswith(FileRef,'/sites/"+this.getSharepointAuth().getSite()+"/SitePages/"+targetRelativeFolder+"') and Title ne null");

        HttpGet request = generateRequest(HttpGet.class, "_api/web/lists/getbytitle('" + encodedTargetFolder + "')/Items?$select=*,FileRef&$filter="+encodedW);

        JSONObject json = executeQuery(request);

        JSONArray results = ((org.json.JSONArray)((org.json.JSONObject)json.get("d")).get("results"));

        return results;
    }

    public Boolean folderExists(String targetFolder) {
        LOG.debug("folderExists ... target:{}", targetFolder);
        String encodedTargetFolder = encode(targetFolder);

        HttpGet request = generateRequest(HttpGet.class, "_api/web/GetFolderByServerRelativeUrl('" + encodedTargetFolder + "')/Exists");

        JSONObject json = executeQuery(request);
        Boolean exists = (Boolean)((JSONObject)json.get("d")).get("Exists");

        return exists;
    }

    public List<JSONObject> createFolderRecursively(String targetFolder, String newFolderTree) {
        LOG.debug("createFolderRecursively ... target:{} new:{}", targetFolder, newFolderTree);

        String[] folders = newFolderTree.split("/");
        List<JSONObject> responses = new ArrayList<JSONObject>();
        String currentPath = targetFolder;

        for(String folder: folders) {
            responses.add(createFolder(currentPath, folder));
            currentPath = currentPath+"/"+folder;
        }

        return responses;
    }

    public JSONObject createFolder(String targetFolder, String newFolder) {
        LOG.debug("createFolder ... target:{} new:{}", targetFolder, newFolder);
        if (newFolder.contains("/")) {
            return createFolderRecursively(targetFolder, newFolder).get(0); // meh! only return the first
        }

        String encodedTargetFolder = encode(targetFolder);
        String encodedNewFolder= encode(newFolder);

        JSONObject meta = new JSONObject();
        meta.put("type", "SP.Folder");

        JSONObject payload = new JSONObject();
        payload.put("__metadata", meta);
        payload.put("ServerRelativeUrl", encodedTargetFolder + "/" + encodedNewFolder);

        HttpPost request = generateRequest(HttpPost.class, "_api/web/GetFolderByServerRelativeUrl('" + encodedTargetFolder + "')/folders", true);
        request.setEntity(stringiFyPayload(payload));

        JSONObject json = executeQuery(request);

        return json;
    }

    public JSONObject uploadFile(String targetFolder, File file) throws Exception {
        LOG.debug("Uploading file... {}", file.getName());
        String encodedTargetFolder = encode(targetFolder);
        String encodedFileName = encode(file.getName());

        HttpPost request = generateRequest(HttpPost.class, "_api/web/GetFolderByServerRelativeUrl('" + encodedTargetFolder + "')/Files/add(url='"
                        + encodedFileName + "',overwrite=true)", true);

        request.removeHeaders("Content-Type");
        request.addHeader(new BasicHeader("Content-Type", "multipart/form-data"));

        request.setEntity(new FileEntity(file));
        JSONObject json = executeQuery(request);

        return json;
    }

    public File downloadFile(String targetFolder, String fileName) throws Exception {
        LOG.debug("Download file... {}", fileName);
        String encodedTargetFolder = encode(targetFolder+"/"+fileName);

        HttpGet request = generateRequest(HttpGet.class, "_api/web/GetFileByServerRelativeUrl('/sites/"+this.getSharepointAuth().getSite()+"/" + encodedTargetFolder + "')/$value", true);
        File file = downloadFile(request, fileName);

        return file;
    }

    /**
     *
     * @param templatePath Make sure the template path ends with / if is in a subfolder.
     *                     Or make this an empty string if template is in root folder
     * @param templateName Template name without aspx extension
     * @param newPageName New page name without aspx extension
     *
     * @return resulting sharepoint "d" data
     */
    public JSONObject createNewPageFromTemplate(String templatePath, String templateName, String newPageName) {
        LOG.debug("createNewPageFromTemplate templatePath: {}, templateName: {},  newPageName: {}", templatePath, templateName, newPageName);
        String encodedPageName = encode(newPageName);

        HttpPost request = generateRequest(HttpPost.class,
                "_api/web/GetFileByServerRelativeUrl('/sites/"+this.getSharepointAuth().getSite()+"/SitePages/"+templatePath+templateName+".aspx')/" +
                        "copyto('/sites/"+this.getSharepointAuth().getSite()+"/SitePages/"+encodedPageName+".aspx')", true);

        JSONObject json = executeQuery(request);

        return json;
    }

    public JSONObject getPage(String pageName) {
        LOG.debug("getPage page... {}", pageName);

        String encodedPageName = encode(pageName);
        HttpGet request = generateRequest(HttpGet.class,
                "_api/web/GetFileByServerRelativeUrl('/sites/"+this.getSharepointAuth().getSite()+"/SitePages/"+encodedPageName+".aspx')/" +
                        "listItemAllFields?$select=CanvasContent1,LayoutWebpartsContent,ID,Title");

        JSONObject json = executeQuery(request);

        return json;
    }

    public JSONObject checkOutPage(Integer pageId) {
        LOG.debug("checkOutPage pageName... {}", pageId);
        HttpPost request = generateRequest(HttpPost.class, String.format("_api/sitePages/pages/GetById(%s)/CheckoutPage",pageId), true);

        JSONObject json = executeQuery(request);

        return json;
    }

    public JSONObject savePage(Integer pageId, JSONObject checkedOut, String HTML, String title) {
        HttpPost request = generateRequest(HttpPost.class, String.format("_api/sitePages/pages/GetById(%s)/SavePageAsDraft", pageId), true);

        JSONParser parser = new JSONParser();
        JSONObject res = (JSONObject)checkedOut.get("d");
        String canvas = (String)res.get("CanvasContent1");
        JSONObject data = new JSONObject();

        try {
            org.json.simple.JSONArray canvasOBJ = (org.json.simple.JSONArray) parser.parse(canvas);
            // for now hardcode the get(0), we assume content is in first web part
            ((org.json.simple.JSONObject)canvasOBJ.get(0)).put("innerHTML", HTML);
            data.put("CanvasContent1", canvasOBJ.toJSONString());
            data.put("BannerImageUrl", res.get("BannerImageUrl"));
            data.put("Title", title);
            data.put("LayoutWebpartsContent",res.get("LayoutWebpartsContent"));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        JSONObject meta = new JSONObject();
        meta.put("type", "SP.Publishing.SitePage");
        data.put("__metadata", meta);

        request.setEntity(stringiFyPayload(data));
        JSONObject json = executeQuery(request);

        return json;
    }

    public JSONObject publishPage(Integer pageId) {
        HttpPost request = generateRequest(HttpPost.class, String.format("_api/sitePages/pages/GetById(%s)/Publish", pageId), true);

        JSONObject json = executeQuery(request);

        return json;
    }

    public User getUser(String email) {
        List<Map<String, Object>> userEntities = getUserEntities(email);

        if(userEntities != null && userEntities.size() == 1) {
            return entityDataToUser(userEntities.get(0));
        } else {
            return null;
        }
    }

    public List<User> getUsers(String email) {
        if(email.length() < MIN_USERS_QUERY_LENGTH) {
            return null;
        }

        List<Map<String, Object>> userEntities = getUserEntities(email);
        List<User> users = new ArrayList<>();

        if(userEntities != null && userEntities.size() > 0) {
            for(int i=0; i<userEntities.size(); i++) {
                HashMap<String, Object> entityData = (HashMap<String, Object>)userEntities.get(i);
                users.add(entityDataToUser(entityData));
            }

            return users.stream().filter(user-> user.getEmail() != null && !user.getEmail().equals("")).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    private List<Map<String, Object>> getUserEntities(String email) {
        HttpPost request = generateRequest(HttpPost.class, String.format("_api/SP.UI.ApplicationPages.ClientPeoplePickerWebServiceInterface.ClientPeoplePickerResolveUser"), true);

        JSONObject data = new JSONObject();
        JSONObject user = new JSONObject();

        data.put("AllowEmailAddresses", true);
        data.put("MaximumEntitySuggestions", 2500);
        data.put("PrincipalSource", 15);
        data.put("PrincipalType", 15);
        data.put("QueryString", email);

        user.put("queryParams", data);

        request.setEntity(stringiFyPayload(user));

        JSONObject json = executeQuery(request);

        String result = (String) ((org.json.JSONObject) json.get("d")).get("ClientPeoplePickerResolveUser");
        List<Map<String, Object>> userEntities = new ArrayList<>();

        try {
            Map<String, Object> mapping = new ObjectMapper().readValue(result, HashMap.class);
            ArrayList test = (ArrayList) mapping.get("MultipleMatches");

            if(mapping.size() == 0) {
                LOG.error("No user was found in sharepoint by [{}] was found :(", email);
                return null;
            }

            if(test.size() > 1) {
                LOG.info("More than one user found");
                return test;
            } else {
                userEntities.add(mapping);
                return userEntities;
            }
        } catch (JsonProcessingException e) {
            LOG.error("Can't process user, will continue...");
            e.printStackTrace();
        }

        LOG.error("Could not retrieve users...");

        return null;
    }

    public JSONObject ensureUser(String text) {
        HttpPost request = generateRequest(HttpPost.class, String.format("_api/Web/EnsureUser('%s')", text), true);

        JSONObject json = executeQuery(request);

        return json;
    }

    public JSONObject shareSite(String userEmailToShareWith, String textInEmail) {
        HttpPost request = generateRequest(HttpPost.class, String.format("_api/SP.Web.ShareObject"), true);

        Map<String, Object> userEntity = getUserEntities(userEmailToShareWith).get(0);
        JSONObject data = new JSONObject();
        org.json.JSONArray ue = new  org.json.JSONArray();
        ue.put(userEntity);

        data.put("url", this.getSharepointAuth().getFullSiteURL());
        data.put("peoplePickerInput", ue.toString());
        data.put("roleValue", "4");
        data.put("sendEmail", true);
        data.put("emailBody", textInEmail);
        data.put("includeAnonymousLinkInEmail", false);
        data.put("useSimplifiedRoles", true);

        request.setEntity(stringiFyPayload(data));

        JSONObject json = executeQuery(request);

        return json;
    }

    public List<User> getUserList(List<String> emails) {
        return emails.stream().map(email-> getUser(email)).collect(Collectors.toList());
    }
}