var common = require("../common");
var IP = common.IP;
var axios = common.axios;
var assert = common.assert;
let logAdmin = common.logAdmin;
let logSuper = common.logSuper;
let logProgrammer = common.logProgrammer;
let logViewer = common.logViewer;
let logOperator = common.logOperator;


describe("4457- Editing", function () {
    it("4483- Admins should be able to edit every user", async function () {
        this.timeout(10000);
        try {
            let response = await logAdmin();
        } catch (error) {
            throw (error);
        }
        try {
            response = await logSuper();
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.put('http://' + IP + ':1207/cs/api/user',
                {
                    username: "admin",
                    password: "ADMIN2",
                    fullName: "admin",
                    permission: "0"
                }, {
                headers: {
                    Authorization: 'Token ' + adminToken
                }
            });
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
            response = await axios.post('http://' + IP + ':1207/cs/api/users', { user: { username: "admin", password: "ADMIN2" } })
            adminToken = response.data.token;
            try {
                assert.equal(response.status, 200);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.put('http://' + IP + ':1207/cs/api/user',
                {
                    username: "admin",
                    password: "ADMIN",
                    fullName: "admin",
                    permission: "0"
                }, {
                headers: {
                    Authorization: 'Token ' + superToken
                }
            });
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
            response = await axios.post('http://' + IP + ':1207/cs/api/users', { user: { username: "admin", password: "ADMIN" } })
            adminToken = response.data.token;
            try {
                assert.equal(response.status, 200);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
    })
    it("4484-Users should be able to edit their own user info", async function () {
        try {
            let response = await logProgrammer();
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.put('http://' + IP + ':1207/cs/api/user',
                {
                    username: "PROGRAMMER",
                    password: "PROGRAMMER2",
                    fullName: "PROGRAMMER",
                    permission: "1"
                }, {
                headers: {
                    Authorization: 'Token ' + programmerToken
                }
            });
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
            response = await axios.post('http://' + IP + ':1207/cs/api/users', { user: { username: "PROGRAMMER", password: "PROGRAMMER2" } })
            programmerToken = response.data.token;
            try {
                assert.equal(response.status, 200);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.put('http://' + IP + ':1207/cs/api/user',
                {
                    username: "PROGRAMMER",
                    password: "1234",
                    fullName: "programmer",
                    permission: "1"
                }, {
                headers: {
                    Authorization: 'Token ' + programmerToken
                }
            });
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, true);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
    })

    it("4485- Users should NOT be able to edit other users", async function () {
        try {
            let response = await logProgrammer();
            response = await logOperator();
            response = await logViewer();
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.put('http://' + IP + ':1207/cs/api/user',
                {
                    username: "VIEWER",
                    password: "VIEWER2",
                    fullName: "viewer",
                    permission: "3"
                }, {
                headers: {
                    Authorization: 'Token ' + programmerToken
                }
            });
            return Promise.reject(new Error("A programmer shouldn't be able to edit a viewer "), response.status);
        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await axios.put('http://' + IP + ':1207/cs/api/user',
                {
                    username: "PROGRAMMER",
                    password: "PROGRAMMER2",
                    fullName: "PROGRAMMER",
                    permission: "1"
                }, {
                headers: {
                    Authorization: 'Token ' + operatorToken
                }
            });
            return Promise.reject(new Error("An operator shouldn't be able to edit a programmer "), response.status);
        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await axios.put('http://' + IP + ':1207/cs/api/user',
                {
                    username: "OPERATOR",
                    password: "OPERATOR2",
                    fullName: "OPERATOR",
                    permission: "2"
                }, {
                headers: {
                    Authorization: 'Token ' + viewerToken
                }
            });
            return Promise.reject(new Error("A viewer shouldn't be able to edit an operator "), response.status);
        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            } catch (error) {
                throw (error);
            }
        }
    })
    it("4486- Non-users should not be able to use this API", async function () {
        try {
            let response = await axios.put('http://' + IP + ':1207/cs/api/user',
                {
                    username: "PROGRAMMER",
                    password: "PRORAMMER2",
                    fullName: "programmer",
                    permission: "1"
                });
            return Promise.reject(new Error("A request without an authorization token shouldn't be successful "), response.status);
        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await axios.put('http://' + IP + ':1207/cs/api/user',
                {
                    username: "PROGRAMMER",
                    password: "PROGRAMMER2",
                    fullName: "PROGRAMMER",
                    permission: "1"
                }, {
                headers: {
                    Authorization: 'Token ' + 'ABCDE'
                }
            });
            return Promise.reject(new Error("A request with a bad authorization token shouldn't be successful "), response.status);
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