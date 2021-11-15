import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;


public class RestTest {
    final String Credentials = "front_2d6b0a8391742f5d789d7d915755e09e:";
    final String BaseURI = "http://test-api.d6.dev.devcaz.com/v2";

    public static int getRandomNumber() {
        int min = 1;
        int max = 100_000;
        return (int) ((Math.random() * (max - min)) + min);
    }
    public static String encodedCredentials(String credentials) {
        byte[] encodedCredentials = Base64.encodeBase64(credentials.getBytes());
        String encodedCredentialsAsString = new String(encodedCredentials);
        return encodedCredentialsAsString;
    }
    public Response getAccessToken() {
        //form body for grant_type=client_credentials
        HashMap<String, String> postContent = new HashMap<>();
        postContent.put("grant_type", "client_credentials");
        postContent.put("scope", "guest:default");
        String encodedCredentialsAsString = encodedCredentials(Credentials);

        //form request
        RequestSpecification request = given();
        Response response = (Response) request.baseUri(BaseURI)
                .basePath("/oauth2/token")
                .contentType(ContentType.JSON)
                .headers("Authorization", "Basic " + encodedCredentialsAsString)
                .with().body(postContent)
                .when().post();
        return response;
    }
    public Response createPlayer(String username, String password, String accessToken) {
        Response response;
        if (accessToken.isEmpty()) {
            response = getAccessToken();
            String jsonResponse = response.getBody().asString();
            accessToken = JsonPath.from(jsonResponse).get("access_token");
        }
        //form data for create player
        String email = "email" + getRandomNumber() + "@example.com";
        HashMap<String, String> postContent = new HashMap<>();
        postContent.put("username", username);
        postContent.put("password_change", password);
        postContent.put("password_repeat", password);
        postContent.put("email", email);
        postContent.put("name", "namejanedoe");
        postContent.put("surname", "namejanedoe");

        //create player
        RequestSpecification request = given();
        response = (Response) request.baseUri(BaseURI)
                .basePath("/players")
                .contentType(ContentType.JSON)
                .headers("Authorization", "Bearer " + accessToken)
                .with().body(postContent)
                .when().post();
        return response;
    }
    public Response authorizePlayer(String username, String password) {
        Response response;
        //Authorize using credentials
        String encodedCredentialsAsString = encodedCredentials(Credentials);

        HashMap<String, String> postContent = new HashMap<>();
        postContent.put("grant_type", "password");
        postContent.put("username", username);
        postContent.put("password", password);

        RequestSpecification request = given();
        response = (Response) request.baseUri(BaseURI)
                .basePath("/oauth2/token")
                .contentType(ContentType.JSON)
                .headers("Authorization", "Basic " + encodedCredentialsAsString)
                .with().body(postContent)
                .when().post();
        assertEquals(200, response.statusCode());
        return response;
    }

    @Test
    public void getGuestAccessToken() {
        Response response = getAccessToken();
        assertEquals(200, response.statusCode());

        String jsonResponse = response.getBody().asString();
        String accessToken = JsonPath.from(jsonResponse).get("access_token");
        assertFalse(accessToken.isEmpty());
    }

    @Test
    public void createPlayer() {
        String username = "user" + getRandomNumber();
        String password = "amFuZWRvZTEyMw==";

        Response response = createPlayer(username, password, "");
        assertEquals(201, response.statusCode());

        String jsonResponse = response.getBody().asString();

        int id = JsonPath.from(jsonResponse).get("id");
        boolean bonusesAllowed = JsonPath.from(jsonResponse).get("bonuses_allowed");
        boolean isVerified = JsonPath.from(jsonResponse).get("is_verified");

        assertTrue( id > 0);
        assertTrue(bonusesAllowed);
        assertFalse(isVerified);
    }

    @Test
    public void authorizePlayer() {
        String username = "user" + getRandomNumber();
        String password = "amFuZWRvZTEyMw==";
        Response response = createPlayer(username, password, "");

        response = authorizePlayer(username, password);

        assertEquals(200, response.statusCode());
        String jsonResponse = response.getBody().asString();

        String accessToken = JsonPath.from(jsonResponse).get("access_token");
        assertFalse(accessToken.isEmpty());

        String refreshToken = JsonPath.from(jsonResponse).get("refresh_token");
        assertFalse(refreshToken.isEmpty());
    }

    @Test
    public void getPlayerProfile() {
        String username = "user" + getRandomNumber();
        String password = "amFuZWRvZTEyMw==";

        Response response = createPlayer(username, password, "");
        String jsonResponse = response.getBody().asString();
        int id = JsonPath.from(jsonResponse).get("id");

        response = authorizePlayer(username, password);
        jsonResponse = response.getBody().asString();
        String accessToken = JsonPath.from(jsonResponse).get("access_token");

        //get Player Info
        RequestSpecification request = given();
        response = (Response) request.baseUri(BaseURI)
                .basePath("/players/" + id)
                .contentType(ContentType.JSON)
                .headers("Authorization", "Bearer " + accessToken)
                .when().get();
        jsonResponse = response.getBody().asString();

        int returnedId = JsonPath.from(jsonResponse).get("id");
        String returnedUsername = JsonPath.from(jsonResponse).get("username");
        assertEquals(id, returnedId);
        assertEquals(username, returnedUsername);
    }

    @Test
    public void getPlayerProfileWithoutAuthorization() {

        //Create first player
        String username = "user" + getRandomNumber();
        String password = "amFuZWRvZTEyMw==";
        Response response = createPlayer(username, password, "");
        String jsonResponse = response.getBody().asString();
        //save ID for the first player
        int firstId = JsonPath.from(jsonResponse).get("id");

        //Create second player
        username = "user" + getRandomNumber();
        password = "amFuZWRvZTEyMw==";
        response = createPlayer(username, password, "");
        jsonResponse = response.getBody().asString();
        //save ID for the second player
        int secondId = JsonPath.from(jsonResponse).get("id");

        response = authorizePlayer(username, password);
        jsonResponse = response.getBody().asString();
        String accessToken = JsonPath.from(jsonResponse).get("access_token");

        //get info for the first player using access_token from the second player
        RequestSpecification request = given();
        response = (Response) request.baseUri(BaseURI)
                .basePath("/players/" + firstId)
                .contentType(ContentType.JSON)
                .headers("Authorization", "Bearer " + accessToken)
                .when().get();
        assertEquals(404, response.statusCode());
    }
}

