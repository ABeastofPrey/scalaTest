var common = require("../common");
var IP = common.IP;
var axios = common.axios;
var assert = common.assert;
let logAdmin = common.logAdmin;
let DeleteUser = common.DeleteUser;

function logTemp() {
    return axios.post('http://' + IP + ':1207/cs/api/users', { user: { username: "TEMP", password: "1234" } });
}
function signTemp(adminToken) {
    return axios.post('http://' + IP + ':1207/cs/api/signup', {
        username: "TEMP",
        password: "1234",
        fullName: "TEMP",
        permission: "1"
    }, {
        headers: {
            Authorization: 'Token ' + adminToken
        }
    });
}

describe("4458- Terms Approval", function () {

    it("4507- Users should be able to agree to the terms and conditions (for their own username)", async function () {
        let tempToken;
        try {
            let response = await logAdmin();
        } catch (error) {
            throw (error);
        }
        try {
            response = await signTemp(adminToken);
            try {
                assert.equal(response.status, 200);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await logTemp();
            tempToken = response.data.token;
            try {
                assert.equal(response.data.user.license, false);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.put("http://" + IP + ":1207/cs/api/license", { username: "TEMP" }, { headers: { Authorization: 'Token ' + tempToken } });
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, true);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await logTemp();
            try {
                assert.equal(response.data.user.license, true);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await DeleteUser("TEMP", adminToken);
            try {
                assert.equal(response.status, 200);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
    })
    it("4508-Users should NOT be able to agree to the terms and conditions for other users", async function () {
        try {
            let response = response = await axios.put("http://" + IP + ":1207/cs/api/license", { username: "PROGRAMMER" }, { headers: { Authorization: 'Token ' + adminToken } });
            return Promise.reject(new Error("An admin shouldn't be able to agree to the terms and conditions for a programmer "), response.status);
        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            } catch (error) {
                throw (error);
            }
        }
    })
    it("4509- Only authenticated users can access this API ", async function () {
        try {
            let response = await axios.put("http://" + IP + ":1207/cs/api/license", { username: "PROGRAMMER" });
            return Promise.reject(new Error("An attempt without a token shouldn't be successful "), response.status);
        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await axios.put("http://" + IP + ":1207/cs/api/license", { username: "PROGRAMMER" }, { headers: { Authorization: 'Token ' + 'ABCDE' } });
            return Promise.reject(new Error("An attempt with a bad token shouldn't be successful "), response.status);
        } catch (error) {
            response = error.response;
            try {
                assert.equal(response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
    })
})