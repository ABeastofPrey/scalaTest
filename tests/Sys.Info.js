var common = require("./common")
var IP = common.IP;
let logOperator = common.logOperator;
let logProgrammer = common.logProgrammer;
let logViewer = common.logViewer;
let logAdmin = common.logAdmin;
var axios = common.axios;
var assert = common.assert;
let logSuper = common.logSuper;

describe("4491- Sys.info", function () {
    it("4492- Users should be able to get sys.info as JSON", async function () {
        try {
            response = await logAdmin();
            response = await logProgrammer();
            response = await logOperator();
            response = await logViewer();
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/sysinfo", {
                headers: {
                    Authorization: 'Token ' + operatorToken
                }
            });
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data.features[0], 'softTP');
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
    })
    it("4493- Non-users should not be able to get sys.info", async function () {
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/sysinfo");
            return Promise.reject
                (new Error("A request to get the Sys.info with no authorization token shouldn't be successful, the response status is: " + response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/sysinfo", {
                headers: {
                    Authorization: 'Token ' + "ABCDE"
                }
            });
            return Promise.reject
                (new Error("A request to get the Sys.info with a bad authorization token shouldn't be successful, the response status is: " + response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            } catch (error) {
                throw (error);
            }
        }
    })
})