var common = require("./common")
var IP = common.IP;
let logOperator = common.logOperator;
let logProgrammer = common.logProgrammer;
let logViewer = common.logViewer;
let logAdmin = common.logAdmin;
var axios = common.axios;
var assert = common.assert;
let logSuper = common.logSuper;
const FormData = require('form-data');
const request = require('request');
const fs = common.fs;

describe("4502- Recordings", function () {
    const badToken = "ABCDE";
    it("4503- Users should be able to get a list of the REC files in the /RAM folder", async function () {
        this.timeout(10000);
        try {
            response = await logAdmin();
            response = await logProgrammer();
            response = await logOperator();
            response = await logViewer();
        } catch (error) {
            throw (error);
        }
        try {
            newFile = fs.createReadStream('MYREC.REC');
            formData = { file: newFile };
            url = "http://" + IP + ":1207/cs/api/uploadRec?token=" + adminToken;
            response = await new Promise((resolve, reject) => {
                request.post({ url: url, formData: formData }, function (err, httpResponse, body) {
                    if (err) {
                        reject(err);
                    }
                    try {
                        assert.equal(httpResponse.statusCode, 200);
                        resolve();
                    }
                    catch (error) {
                        reject(error);
                    }
                })
            })
        }
        catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/dashboard/recfiles", {
                headers: {
                    Authorization: 'Token ' + programmerToken
                }
            });
            assert.equal(response.data[0], 'MYREC.REC')
        } catch (error) {
            throw (error);
        }
    })
    it("4504- Non-users should NOT be able to get list of REC files", async function () {
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/dashboard/recfiles");
            return Promise.reject(new Error
                ("A request to get the REC files with no authorization header shouldn't be successful, the response status is: " + response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/dashboard/recfiles", {
                headers: {
                    Authorization: 'Token ' + badToken
                }
            });
            return Promise.reject(new Error
                ("A request to get the REC files with a bad authorization header shouldn't be successful, the response status is: " + response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            } catch (error) {
                throw (error);
            }
        }
    })
    it("4505- Users should be able to get a rec file as CSV", async function () {
        this.timeout(10000);
        try {
            response = await logAdmin();
            response = await logProgrammer();
            response = await logOperator();
            response = await logViewer();
        } catch (error) {
            throw (error);
        }
        try {
            newFile = fs.createReadStream('MYREC.REC');
            formData = { file: newFile };
            url = "http://" + IP + ":1207/cs/api/uploadRec?token=" + adminToken;
            response = await new Promise((resolve, reject) => {
                request.post({ url: url, formData: formData }, function (err, httpResponse, body) {
                    if (err) {
                        reject(err);
                    }
                    try {
                        assert.equal(httpResponse.statusCode, 200);
                        resolve();
                    }
                    catch (error) {
                        reject(error);
                    }
                })
            })
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/dashboard/rec/MYREC", {
                headers: {
                    Authorization: 'Token ' + programmerToken
                }
            });
            assert.equal(response.data['data'].split('\n')[0], '1');
        } catch (error) {
            throw (error);
        }
    })
    it("4506- Non-users should NOT be able to get REC FILES as CSV", async function () {
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/dashboard/rec/MYREC");
            return Promise.reject(
                new Error("A request to get a REC file as CSV with no authorization token shouldn't be successful, the response status is: ", response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/dashboard/rec/MYREC", {
                headers: {
                    Authorization: 'Token ' + badToken
                }
            });
            return Promise.reject(
                new Error("A request to get a REC file as CSV with a bad authorization token shouldn't be successful, the response status is: ", response.status));
        } catch (error) {
            try {
                assert.equal(error.response.status, 403);
            } catch (error) {
                throw (error);
            }
        }

    })
})