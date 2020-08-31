var common = require("../common");
var IP = common.IP;
var axios = common.axios
var assert = common.assert;
let logProgrammer = common.logProgrammer;
let logAdmin = common.logAdmin;
let logSuper = common.logSuper;
let signProgrammer = common.signProgrammer
let signOperator = common.signOperator;
let logOperator = common.logOperator;
let signViewer = common.signViewer;
let logViewer = common.logViewer;
let DeleteUser = common.DeleteUser;


function validateViewerandAdmin(Array) {
  foundViewer = false;
  foundAdmin = false;
  for (i = 0; i < 2; i++) {
    if (Array[i].username == "VIEWER") {
      foundViewer = true;
    }
    if (Array[i].username == "admin") {
      foundAdmin = true;
    }
  }
  return (foundAdmin && foundViewer);
}

describe("4455- Delete", function () {

  it("4473- Admin users should be able to delete users", async function () {
    try {
      let response = await logAdmin();
    } catch (error) {
      throw (error);
    }
    try {
      response = await DeleteUser("PROGRAMMER", adminToken);
      try {
        assert.equal(response.data, true);
        assert.equal(response.status, 200);
      } catch (error) {
        throw (error);
      }
    } catch (error) {
      throw (error);
    }
    try {
      let response = await logSuper();
    } catch (error) {
      throw (error);
    }
    try {
      response = await DeleteUser("OPERATOR", superToken);
      try {
        assert.equal(response.data, true);
        assert.equal(response.status, 200);
      } catch (error) {
        throw (error);
      }
    } catch (error) {
      throw (error);
    }
    try {
      response = await axios.get("http://" + IP + ":1207/cs/api/users", {
        headers: {
          Authorization: 'Token ' + superToken
        }
      });
      assert.equal(validateViewerandAdmin(response.data), true)
    } catch (error) {
      throw (error);
    }
  })
  it("4474- Non-admin users should NOT be able to delete users", async function () {
    this.timeout(10000);
    try {
      response = await signProgrammer(adminToken); // SIGNING THE PROGRAMMER AFTER HE WAS DELETED
      response = await logProgrammer();
    } catch (error) {
      throw (error);
    }
    try {
      response = await signOperator(adminToken); // SIGNING THE OPERATOR AFTER HE WAS DELETED
      response = await logOperator();
    } catch (error) {
      throw (error);
    }
    try {
      response = await signViewer(adminToken);
      response = await logViewer;
    } catch (error) {
      throw (error);
    }
    try {
      response = await DeleteUser("VIEWER", programmerToken);
      return Promise.reject(new Error("A programmer shouldn't be able to delete a viewer "), response.status);
    } catch (error) {
      try {
        assert.equal(error.response.status, 403);
      } catch (error) {
        throw (error)
      }
    }
    try {
      response = await DeleteUser("VIEWER", operatorToken);
      return Promise.reject(new Error("An operator shouldn't be able to delete a viewer "), response.status);
    } catch (error) {
      try {
        assert.equal(error.response.status, 403);
      } catch (error) {
        throw (error)
      }
    }
    try {
      response = await DeleteUser("VIEWER", viewerToken);
      return Promise.reject(new Error("A viewer user shouldn't be able to delete himself "), response.status);
    } catch (error) {
      try {
        assert.equal(error.response.status, 403);
      } catch (error) {
        throw (error)
      }
    }
    try {
      response = await axios.delete("http://" + IP + ":1207/cs/api/user/VIEWER");
      return Promise.reject(new Error("A request to delete a user with no authorization header should fail "), response.status);

    } catch (error) {
      try {
        assert.equal(error.response.status, 401);
      } catch (error) {
        throw (error)
      }
    }
  })
  it("4475- No one can delete ADMIN or SUPER users", async function () {
    try {
      response = await DeleteUser("admin", superToken);
      try {
        assert.equal(response.status, 200);
        assert.equal(response.data, false);
      } catch (error) {
        throw (error);
      }
    }
    catch (error) {
      throw (error);
    }
    try {
      response = await DeleteUser("super", superToken);
      try {
        assert.equal(response.status, 200);
        assert.equal(response.data, false);
      } catch (error) {
        throw (error);
      }

    } catch (error) {
      throw (error);
    }
  })
})