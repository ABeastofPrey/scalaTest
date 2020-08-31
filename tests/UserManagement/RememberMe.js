var common = require("../common")
var IP = common.IP;
var axios = common.axios
var assert = common.assert;
let logProgrammer = common.logProgrammer;


describe("4481- Remember me", function () {

  it("4482- Users should be able to retrieve their information based on their token", async function () {
    try {
      let response = await logProgrammer();
    } catch (error) {
      throw (error);
    }
    try {
      response = await axios.get("http://" + IP + ":1207/cs/api/user", {
        headers: {
          Authorization: 'Token ' + programmerToken
        }
      });
      try {
        assert.equal(response.status, 200);
        assert.equal(typeof (response.data), "object");
        assert.equal(response.data.user.license, false);
        assert.equal(response.data.user.fullName, 'PROGRAMMER PROGRAMMER');
        assert.equal(response.data.user.permission, 1);
        assert.equal(response.data.user.username, 'PROGRAMMER');
      } catch (error) {
        throw (error);
      }
    } catch (error) {
      throw (error);
    }
  })
})