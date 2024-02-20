package sharepoint.igt.com;

import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sharepoint.beans.igt.com.User;
import sharepoint.igt.com.connection.SharepointContext;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        // run the library with JVM params as:
        // -Dsite=ATSTerminalSoftwareEngineers -DclientID=YYYYYY-623f-XXXX-951a-DDDDDD -DsecretID=yourSecret
        Long timeMe = new Date().getTime();
        // as an example, this is the site I am working for:
        // --------<        domain         >------<           site            >
        // https://gtechcorp.sharepoint.com/sites/ATSTerminalSoftwareEngineers/
        String domain = "gtechcorp.sharepoint.com";
        String clientID = System.getProperty("clientID"); // provided by our sharepoint admins
        String secretID = System.getProperty("secretID"); // provided by our sharepoint admins
        String site = System.getProperty("site");

        // Here are some examples...

        // this is enough to get SP connection context, we can start adding methods there...
        SharepointContext spc = new SharepointContext(clientID, secretID, domain, site);
        JSONObject json = spc.getAllSiteLists();

        // just drafting ideas...
        JSONArray res =  ((JSONArray)((JSONObject) json.get("d")).get("results"));

        List<JSONObject> jsonObject = IntStream
                .range(0,res.length())
                .mapToObj(i -> res.getJSONObject(i))
                .collect(Collectors.toList());

        // get list123 particularly from all lists
        List<JSONObject> list123 = jsonObject.stream().filter(x -> x.get("Title").equals("test123")).collect(Collectors.toList());
        LOG.debug(list123.get(0).get("Title").toString());

        // let's query for list123
        JSONObject list1234 = spc.getListItemsByTitle("test123");
        JSONArray list123Items =  ((JSONArray)((JSONObject) list1234.get("d")).get("results"));

        // print... :)
        for(Object ob : list123Items) {
            JSONObject a = (JSONObject) ob;
            LOG.debug(a.get("Title").toString());
        }

        // let's upload a file, super simple!
        try {
            spc.uploadFile("Sand Box", new File("hi.txt"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // let's play with folders creation in a library
        String folderName = "newFolder-"+timeMe;

        // create a folder under ReleaseNotes lib
        spc.createFolder("Sand Box", folderName);

        // Check if a folder exists...
        Boolean exists = spc.folderExists("Sand Box/"+folderName);
        LOG.debug("The folder {}, exists?: {}", folderName, exists);

        // create a folder under a certain path!
        String path  = "Sand Box/New York";
        spc.createFolder("SitePages", path); // create folder prior page... (maybe we can include this in the create page from template?)

        String pageName = "testHelloWorld"+timeMe;
        // Create a page with a template already created, if you use a path, make sure you SitePages have the "make folder" option enabled
        spc.createNewPageFromTemplate("Templates/", "RNTemplate", path+"/"+pageName);
        JSONObject page = (JSONObject)spc.getPage(path+"/"+pageName).get("d");

        // Get page ID and checkout
        Integer id = (Integer) page.get("ID");
        JSONObject checkedOutPage = spc.checkOutPage(id);

        // Use all previous data to modify the page and publish
        spc.savePage(id, checkedOutPage, "<title>This title!"+timeMe+"</title><div>this is a test... finally working "+timeMe+"!</div>", "My title ->" + timeMe);
        spc.publishPage(id);

        JSONArray pages = spc.getPagesListByRelativePath(path);
        LOG.debug("The title for the first list of pages is: {}", pages.getJSONObject(0).get("Title"));

        String email = "eduardo.saavedra";
        User usObj = spc.getUser(email);
        LOG.debug("With this information: {} Retrieving user: {}", email, usObj.toString());

        String email2 = "eduar";
        List<User> usArrObj = spc.getUsers(email2);
        LOG.debug("Found [{}] results:", usArrObj.size());
        usArrObj.stream().forEach((user) -> {LOG.debug("Email: [{}], Name: [{}]", user.getEmail(), user.getDisplayName());});

        try {
            File file = spc.downloadFile("Release Notes/txs-rfid/TX Crate RFID Reader/Sprint 2 BTC 17", "release-mail-sprint_2_btc_17.zip");
            LOG.debug("File was successfully downloaded!");
        } catch (Exception e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
