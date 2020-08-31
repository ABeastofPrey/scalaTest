var common = require("./common")
var axios = common.axios
var IP = common.IP;
var assert = common.assert;
var fs = common.fs;
var FormData = common.FormData;
var request = common.request;
var Client = common.Client;
let signupUsers = common.signupUsers;
let logAdmin = common.logAdmin;
let logSuper = common.logSuper;
let sendSSHCommand = common.sendSSHCommand;
let signProgrammer = common.signProgrammer;
let signOperator = common.signOperator;
let signViewer = common.signViewer;




function factoryRestore(token) {
    return axios.get('http://' + IP + ':1207/cs/api/factoryRestore', {
        headers: {
            Authorization: 'Token ' + token
        }
    })
}



describe('Factory Restore', function () {
    before(function (done) {
        this.timeout(10000);
        logAdmin().then(() => {
            done();
        }).catch(err => {
            done(err);
        });
    })

    it("4462 Should restore the webserver to the Factory state", async function () {
        this.timeout(120000);
        let response;
        try {
            response = await sendSSHCommand("pkill java");
        } catch (err) {
            throw (err);
        }
        await new Promise((resolve) => {
            setTimeout(function () {
                resolve();
            }, 30000)
        });
        // DO FACTORY RESTORE WITH ADMIN TOKEN
        try {
            response = await factoryRestore(adminToken);
        } catch (err) {
            throw err;
        }
        // MAKE SURE RESPONSE IS OK...
        try {
            assert.equal(response.status, 200);
        } catch (error) {
            throw error;
        }
        try {
            response = await logAdmin(); //
            // if you reach this line then response is 200...
            return Promise.reject(new Error("response shouldnt be like this", response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 422);
            }
            catch (error) {
                throw (error);
            }
        }
        response = await sendSSHCommand("pkill java");
        await new Promise((resolve) => {
            setTimeout(function () {
                resolve();
            }, 30000)
        });
        try {
            response = await logAdmin();
        }
        catch (error) {
            throw (error);
        }
        try {
            response = await signProgrammer(adminToken);
            response = await signOperator(adminToken);
            response = await signViewer(adminToken);
        } catch (error) {
            throw (error); //An error occured with signing up the users
        }
        try {
            response = await factoryRestore(programmerToken);
            return Promise.reject(new Error("response shouldnt be like this", response.status));

        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            }
            catch (error) {
                throw (error);
            }
        }
        try {
            response = await factoryRestore(operatorToken);
            return Promise.reject(new Error("response shouldnt be like this", response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            }
            catch (error) {
                throw (error);
            }
        }
        try {
            response = await factoryRestore(viewerToken);
            return Promise.reject(new Error("response shouldnt be like this", response.status));

        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            }
            catch (error) {
                throw (error);
            }
        }
        try {
            response = await axios.get('http://' + IP + ':1207/cs/api/factoryRestore')
            return Promise.reject(new Error("A request without a header should fail "), response.status) //A request without a header should fail
        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await logSuper()
        } catch (error) {
            throw (error)
        }
        try {
            response = await factoryRestore(superToken)
            try {
                assert.equal(response.status, 200);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.post('http://' + IP + ':1207/cs/api/users', {
                "user": {
                    username: 'PROGRAMMER',
                    password: '1234'
                }
            });
            return Promise.reject(new Error("An error occured restoring the factory setting with a SUPER token", response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 422); // ???? //
            }
            catch (error) {
                throw (error);
            }
        }
        response = await sendSSHCommand("pkill java");
        await new Promise((resolve) => {
            setTimeout(function () {
                resolve();
            }, 30000)
        });
        try {
            response = await logAdmin()
        }
        catch (error) {
            throw (error)
        }
        try {
            response = await signProgrammer(adminToken);
            response = await signOperator(adminToken);
            response = await signViewer(adminToken);
        } catch (error) {
            throw (error);
        };
    });
})

