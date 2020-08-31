var common = require("./common")
var IP = common.IP;
let logOperator = common.logOperator;
let logProgrammer = common.logProgrammer;
let logViewer = common.logViewer;
let logAdmin = common.logAdmin;
var axios = common.axios;
var assert = common.assert;
let logSuper = common.logSuper;

function getLog(token) {
    return axios.get("http://" + IP + ":1207/cs/api/log", {
        headers: {
            Authorization: 'Token ' + token
        }
    });
}

describe("4459- Server Log", function () {
    it("4471- Admin users should get the system log", async function () {
        this.timeout(10000);
        try {
            let response = await logProgrammer();
            response = await logOperator();
            response = await logViewer();
        } catch (error) {
            throw (error);
        }
        try {
            response = await logAdmin();
            try {
                assert.equal(response.status, 200);
                assert.notEqual(adminToken, null);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await getLog(adminToken);
            try {
                assert.equal(response.status, 200);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            assert.equal(response.data[0].msg, "login");
            assert.equal(response.data[0].username, "admin")
        } catch (error) {
            throw (error);
        }
        try {
            response = await logSuper();
            try {
                assert.equal(response.status, 200);
                assert.notEqual(superToken, null);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await getLog(superToken);
            try {
                assert.equal(response.status, 200);
            } catch (error) {
                throw (error);
            }
        }
        catch (error) {
            throw (error)
        }
        try {
            assert.equal(response.data[0].msg, "login");
            assert.equal(response.data[0].username, "super");
        } catch (error) {
            throw (error);
        }

    });
});