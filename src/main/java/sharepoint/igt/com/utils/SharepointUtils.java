package sharepoint.igt.com.utils;

import org.apache.http.Header;
import sharepoint.beans.igt.com.User;

import java.util.Arrays;
import java.util.Map;

public class SharepointUtils {
    public static String extractHeaderElement(Header[] headers, String elementName) {
        return Arrays.asList(headers).stream()
                .map(header -> header.getElements())
                .flatMap(elements -> Arrays.asList(elements).stream())
                .filter(element -> element.getName().equals(elementName))
                .findFirst()
                .get()
                .getValue();
    }

    public static User entityDataToUser(Map<String, Object> mapping) {
        User userObj = new User();
        Map<String, String> ed = (Map<String, String>)mapping.get("EntityData");

        String title = ed.get("Title");
        String mail = ed.get("Email");
        String displayName = (String) mapping.get("DisplayText");

        userObj.setDisplayName(displayName);
        userObj.setTitle(title);
        userObj.setEmail(mail);

        return userObj;
    }
}
