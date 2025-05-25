package scenario;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ResetPasswordScenarioTest {

    private String baseEmail = "albertjuntak44@gmail.com";
    private String oldPassword = "afteroffice123";
    private String newPassword = "newpassword123";
    private String tokenOld;
    private String tokenNew;

    @BeforeClass
    public void loginWithOldPassword() {
        RestAssured.baseURI = "https://whitesmokehouse.com";

        String jsonLogin = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(baseEmail, oldPassword);

        Response response = RestAssured.given()
                .header("Content-Type", "application/json")
                .body(jsonLogin)
                .post("/webhook/employee/login");

        Assert.assertEquals(response.statusCode(), 200);
        tokenOld = response.jsonPath().getString("[0].token");
        Assert.assertNotNull(tokenOld, "Token login lama null");
    }

    @Test(priority = 1)
    public void resetPassword() {
        String jsonUpdate = """
                {
                    "email": "%s",
                    "password": "%s",
                    "full_name": "Albert Simanjuntak",
                    "department": "QA",
                    "title": "Engineer"
                }
                """.formatted(baseEmail, newPassword);

        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + tokenOld)
                .header("Content-Type", "application/json")
                .body(jsonUpdate)
                .put("/webhook/employee/update");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("[0].password"), null); // password tidak dikembalikan
    }

    @Test(priority = 2, dependsOnMethods = "resetPassword")
    public void loginWithNewPassword() {
        String jsonLogin = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(baseEmail, newPassword);

        Response response = RestAssured.given()
                .header("Content-Type", "application/json")
                .body(jsonLogin)
                .post("/webhook/employee/login");

        Assert.assertEquals(response.statusCode(), 200);
        tokenNew = response.jsonPath().getString("[0].token");
        Assert.assertNotNull(tokenNew, "Token login baru null");
    }

    @Test(priority = 3, dependsOnMethods = "loginWithNewPassword")
    public void getEmployeeDataWithNewToken() {
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + tokenNew)
                .get("/webhook/employee/get");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("[0].email"), baseEmail);
        Assert.assertEquals(response.jsonPath().getString("[0].full_name"), "Albert Simanjuntak");
        Assert.assertEquals(response.jsonPath().getString("[0].department"), "QA");
    }

    @Test(priority = 4, dependsOnMethods = "getEmployeeDataWithNewToken")
    public void revertPasswordToOld() {
        String jsonRevert = """
                {
                    "email": "%s",
                    "password": "%s",
                    "full_name": "Albert Simanjuntak",
                    "department": "QA",
                    "title": "Engineer"
                }
                """.formatted(baseEmail, oldPassword);

        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + tokenNew)
                .header("Content-Type", "application/json")
                .body(jsonRevert)
                .put("/webhook/employee/update");

        Assert.assertEquals(response.statusCode(), 200);
    }
}
