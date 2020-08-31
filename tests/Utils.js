var common = require("./common")
var IP = common.IP;
var axios = common.axios;
var assert = common.assert;
let logAdmin = common.logAdmin;
let logSuper = common.logSuper;
let logProgrammer = common.logProgrammer;
let logOperator = common.logOperator;
let logViewer = common.logViewer;
let sendSSHCommand = common.sendSSHCommand;

describe("4523- Utils", function () {
    it("4524-/cs/trnerr should retrieve contents of trn.err file ", async function () {
        userToken = null;
        try {
            let response = await sendSSHCommand('echo TEST > /RAM/TRN.ERR');
        } catch (error) {
            throw (error);
        }
        try {
            let response = await axios.get('http://' + IP + ':1207/cs/trnerr', {
                headers: {
                    Authorization: 'Token ' + userToken
                }
            });
            try {
                assert.equal(response.data, 'TEST\n');
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
    })
    it("4534- /cs/api/java-version should return value of web server", async function () {
        try {
            let response = await axios.get("http://" + IP + ":1207/cs/api/java-version");
            try {
                assert.equal(response.status, 200);
                assert.equal(typeof (response.data.ver), "string");
                assert.notEqual(response.data.ver, null);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
    })
    it("4535- /cs/api/license/{name} should return if license exists", async function () {
        try {
            let response = await axios.get("http://" + IP + ":1207/cs/api/license/TEST");
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, false);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/license/softTP");
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
    it("4536- /cs/api/theme should return current theme", async function () {
        try {
            let response = await axios.get("http://" + IP + ":1207/cs/api/theme");
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data.theme, 'kuka');
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
    })
    it("4537- /cs/MCCommands and /cs/MCCommands/all  should return all MC-Basic properties/commands", async function () {
        this.timeout(10000);
        try {
            let response = await axios.get("http://" + IP + ":1207/cs/MCCommands");
            try {
                assert.equal(response.status, 200);
                assert.notEqual(response.data[0].text, undefined);
                assert.notEqual(response.data[0].value, undefined);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/MCCommands/all");
            try {
                assert.equal(response.status, 200);
                assert.equal(typeof (response.data), 'string');
                assert.notEqual(response.data, "");
                array = response.data.split("|");
                assert.equal(array[0], 'print');
                assert.equal(array[1], 'printusing');
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
    })
    it("4550- /cs/api/pkgd should return TRUE if pkgd is missing OR if it contains the words \"successfuly\"", async function () {
        try {
            let response = await sendSSHCommand("rm -f /var/home/pkgd.log");
            response = await sendSSHCommand("ls /var/home/pkgd.log");
            assert.equal(response, "ls: /var/home/pkgd.log: No such file or directory");
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/pkgd");
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
            response = await sendSSHCommand("echo Installed successfully >/var/home/pkgd.log");
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/pkgd");
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
            response = await sendSSHCommand("echo >/var/home/pkgd.log");
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/pkgd");
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
    it("4554-Only SUPER user can change theme", async function () {
        this.timeout(10000);
        try {
            let response = await logAdmin();
            response = await logSuper();
            response = await logProgrammer()
            response = await logOperator();
            response = await logViewer();
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.put("http://" + IP + ":1207/cs/api/theme/kuka",{},{
                headers: {
                    Authorization: 'Token ' + adminToken
                }
            });

            return Promise.reject(new Error("An admin shouldn't be able to change the theme, the response status is: " + response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            } catch (error) {
                throw (error);
            }

        }
        try {
            response = await axios.put("http://" + IP + ":1207/cs/api/theme/kuka",{}, {
                headers: {
                    Authorization: 'Token ' + programmerToken
                }
            });
            return Promise.reject(new Error("A programmer shouldn't be able to change the theme, the response status is: " + response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await axios.put("http://" + IP + ":1207/cs/api/theme/kuka");
            return Promise.reject(new Error("An admin shouldn't be able to change the theme, the response status is: " + response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await axios.put("http://" + IP + ":1207/cs/api/theme/kuka",{}, {
                headers: {
                    Authorization: 'Token ' + superToken
                }
            });
            try {
                assert.equal(response.status, 200);
                response = await axios.get("http://" + IP + ":1207/cs/api/theme");
                assert.equal(response.data.theme, "kuka");
            } catch (error) {
                throw (error);

            }
        }
        catch (error) {
            throw (error);
        }
    })
})
