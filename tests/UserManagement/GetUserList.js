var common = require("../common")
var IP = common.IP;
var axios = common.axios
let logAdmin = common.logAdmin;
var assert = common.assert;
let logSuper = common.logSuper;
let logProgrammer = common.logProgrammer;


function findUsers(Array) {
  let foundProgrammer = false;
  let foundAdmin = false;
  let foundOperator = false;
  let foundViewer = false;

  for (i = 0; i < Array.length; i++) {
    if (Array[i].username == "OPERATOR") {
      foundOperator = true;
    }
    if (Array[i].username == "PROGRAMMER") {
      foundProgrammer = true;
    }
    if (Array[i].username == "VIEWER") {
      foundViewer = true;
    }
    if (Array[i].username == "admin") {
      foundAdmin = true;
    }
  }
  if (foundOperator && foundProgrammer && foundViewer && foundAdmin) {
    return true;
  }
  return false;
}

describe("4487- Get user list", function () {
  it("4488- Admin/Super users should get an array with all users", async function () {
    try {
      response = await logAdmin();
    } catch (error) {
      throw (error);
    }
    try {
      response = await axios.get("http://" + IP + ":1207/cs/api/users", {
        headers: {
          Authorization: 'Token ' + adminToken
        }
      });
      try {
        assert.equal(findUsers(response.data), true);
      } catch (error) {
        throw (error);
      }
    } catch (error) {
      throw (error);
    } try {
      response = await logSuper();
    } catch (error) {
      throw (error);
    }
    try {
      response = await axios.get("http://" + IP + ":1207/cs/api/users", {
        headers: {
          Authorization: 'Token ' + superToken
        }
      });
      try {
        assert.equal(findUsers(response.data), true);
      } catch (error) {
        throw (error);
      }
    } catch (error) {
      throw (error);
    }
  })

  it("4489- Normal users should only get a list with their own user", async function () {
    try {
      response = await logProgrammer();
    } catch (error) {
      throw (error);
    }
    try {
      response = await axios.get("http://" + IP + ":1207/cs/api/users", {
        headers: {
          Authorization: 'Token ' + programmerToken
        }
      });
      try {
        let lengthOfArray = response.data.length;
        assert.equal(response.data[0].username, "PROGRAMMER");
        assert.equal(lengthOfArray, 1);

      } catch (error) {
        throw (error);
      }
    } catch (error) {
      throw (error);
    }
  })
  it("4490- Non users should not get the list of users", async function () {
    try {
      response = await axios.get("http://" + IP + ":1207/cs/api/users");
      return Promise.reject(new Error("A get all users request without an authorization token should fail", response.status));
    }
    catch (error) {
      try {
        assert.equal(error.response.status, 401);
      } catch (error) {
        throw (error);
      }
    }
    try {
      response = await axios.get("http://" + IP + ":1207/cs/api/users", {
        headers: {
          Authorization: 'Token ' + "ABCDE"
        }
      });
      throw new Error("A get all users request with a bad authorization token should fail", response.status);
    } catch (error) {
      try {
        assert.equal(error.response.status, 403);
      } catch (error) {
        throw (error);
      }
    }
  })

})