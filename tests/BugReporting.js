var common = require("./common")
var axios = common.axios
var IP = common.IP;
var assert = common.assert;
var fs = common.fs;
let signupUsers = common.signupUsers;
let logAdmin = common.logAdmin;
let logSuper = common.logSuper;
let sendSSHCommand = common.sendSSHCommand;
let logProgrammer = common.logProgrammer;
let logViewer = common.logViewer;
let logOperator = common.logOperator;

describe("4531- Bug Reporting", function () {
    it("4532- /cs/api/bugreport should allow authenticated users to create a bug-report ZIP", async function () {
        this.timeout(10000);
        try {
            let response = await logAdmin();
            response = await logProgrammer();
            response = await logOperator();
            response = await logViewer();
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.post("http://" + IP + ":1207/cs/api/bugreport",
                {
                    user: "A",
                    info: "B",
                    history: "C"
                }, {
                headers: {
                    Authorization: 'Token ' + operatorToken
                }
            });
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, true);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            console.log(error)
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/zipSysFile")
            try {
                assert.equal(response.status, 200);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
    })
    it("4533- /cs/api/bugReport should NOT allow un-authenticated users to create a bug-report ZIP", async function () {
        try {
            let response = await axios.post("http://" + IP + ":1207/cs/api/bugreport", {
                user: "A",
                info: "B",
                history: "C"
            });
            return Promise.reject(new Error("A non authenticated user shouldn't be able to create a bug report, the response status is: " + response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await axios.post("http://" + IP + ":1207/cs/api/bugreport",
                {
                    user: "A",
                    info: "B",
                    history: "C"
                }, {
                headers: {
                    Authorization: 'Token ' + "ABCDE"
                }
            });
            return Promise.reject(new Error("A poorly authenticated user shouldn't be able to create a bug report, the response status is: " + response.status));

        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            } catch (error) {
                throw (error);
            }

        }

    })

})