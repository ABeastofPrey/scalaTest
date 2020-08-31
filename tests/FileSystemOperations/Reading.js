var common = require("../common")
var IP = common.IP;
var axios = common.axios
var assert = common.assert;
let resetFileSystem = common.resetFileSystem;
let logAdmin = common.logAdmin;
let logSuper = common.logSuper;
let logProgrammer = common.logProgrammer;
let logOperator = common.logOperator;
let logViewer = common.logViewer;

describe("4546-Reading", function () {
    describe("4548-/cs/path", function () {
        it("4549- API should get the contents of a file inside a folder", async function () {
            try {
                let response = await resetFileSystem();
            } catch (error) {
                throw (error);
            }
            try {
                response = await axios.post("http://" + IP + ":1207/cs/api/move", {
                    target: "MYFOLDER",
                    files: "MYPRG.PRG"
                });
            } catch (error) {
                throw (error);
            }
            try {
                response = await axios.get("http://" + IP + ":1207/cs/api/files");
                try {
                    assert.equal(response.data.children[0].files[0], 'MYPRG.PRG');
                } catch (error) {
                    throw (error);
                }
            } catch (error) {
                throw (error);
            }

            try {
                let path = { params: { path: "MYFOLDER/MYPRG.PRG" } };
                response = await axios.get("http://" + IP + ":1207/cs/path", path);
                try {
                    assert.equal(response.data, "hello\n");
                    assert.equal(response.status, 200);
                } catch (error) {
                    throw (error);
                }
            } catch (error) {
                throw (error);
            }
        })
    })
    describe("4543- /cs/file/{filename}", function () {
        it("4544- API should return the contents of a file in SSMC", async function () {
            try {
                let response = logAdmin();
            } catch (error) {
                throw (error)
            }
            try {
                let response = await resetFileSystem();
            } catch (error) {
                throw (error);
            }
            try {
                response = await axios.get("http://" + IP + ":1207/cs/file/MYPRG.PRG", {
                    headers: {
                        Authorization: 'Token ' + adminToken
                    }
                });
                try {
                    assert.equal(response.data, 'hello\n');
                    assert.equal(response.status, 200);
                } catch (error) {
                    throw (error);
                }
            } catch (error) {
                throw (error);
            }
        }),
            it("4545- Only Use API with ADMIN token to ask for FWCONFIGAdmins should be able to read FWCONFIG", async function () {
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
                    response = await axios.get("http://" + IP + ":1207/cs/file/FWCONFIG");
                    return Promise.reject(new Error("A request for FWCONFIG without a token shouldn't be successful, the response status is: " + response.status));
                } catch (error) {
                    try {
                        assert.equal(error.response.status, 401);
                    } catch (error) {
                        throw (error);
                    }
                }
                try {
                    response = await axios.get("http://" + IP + ":1207/cs/file/FWCONFIG", {
                        headers: {
                            Authorization: 'Token ' + programmerToken
                        }
                    });
                    return Promise.reject(new Error("A programmer's request to get the FWCONFIG shouldn't be successful, the response status is: " + response.status));
                } catch (error) {
                    try {
                        assert.equal(error.response.status, 403);
                    } catch (error) {
                        throw (error);
                    }
                }
                try {
                    response = await axios.get("http://" + IP + ":1207/cs/file/FWCONFIG", {
                        headers: {
                            Authorization: 'Token ' + adminToken
                        }
                    });
                    try {
                        assert.equal(response.status, 200);
                        assert.equal(typeof (response.data), 'string');
                        assert.notEqual(response.data, "");
                    } catch (error) {
                        throw (error);
                    }
                } catch (error) {
                    throw (error)
                }

            })
    })


})