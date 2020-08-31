var common = require("./common")
var IP = common.IP;
var axios = common.axios;
var assert = common.assert;
const FormData = require('form-data');
const request = require('request');
const fs = common.fs;

function lookForDemo(array) {
    for (i = 0; i < array.length; i++) {
        if (array[i].path == 'DEMO') {
            return true;
        }
    }
    return false;
}
describe("4528- Project backup/restore", function () {
    it("4529- /cs/api/verifyProject should return if the uploaded zip file is a valid project backup file", async function () {
        this.timeout(15000);
        let returnedProjectFileName;
        newFile = fs.createReadStream('INVALIDZIPFILE.ZIP');
        formData = { file: newFile };
        const url = "http://" + IP + ":1207/cs/api/verifyProject"
        try {
            let response = await new Promise((resolve, reject) => {
                request.post({ url: url, formData: formData }, function (err, httpResponse, body) {
                    if (err) {
                        reject(err);
                    }
                    try {
                        assert.equal(httpResponse.statusCode, 200);
                        assert.equal(JSON.parse(httpResponse.body).success, false);
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
            newFile = fs.createReadStream('VALIDZIPFILE.ZIP');
            formData = { file: newFile };
            response = await new Promise((resolve, reject) => {
                request.post({ url: url, formData: formData }, function (err, httpResponse, body) {
                    if (err) {
                        reject(err);
                    }
                    try {
                        assert.equal(httpResponse.statusCode, 200);
                        assert.equal(JSON.parse(httpResponse.body).success, true);
                        assert.equal(JSON.parse(httpResponse.body).project, "DEMO");
                        returnedProjectFileName = JSON.parse(httpResponse.body).file;
                        resolve();
                    }
                    catch (error) {
                        resolve(error);
                    }
                })
            })
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/importProject", { params: { fileName: returnedProjectFileName } });
            try {
                assert.equal(response.data.success, true);
                response = await axios.get("http://" + IP + ":1207/cs/api/files");
                assert.equal(lookForDemo(response.data.children), true);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.delete("http://" + IP + ":1207/cs/api/projectZip", { params: { fileName: "INVALIDZIPFILE.ZIP" } });
            try {
                assert.equal(response.data.success, false);
                assert.equal(response.status, 200);
            } catch (error) {
                throw (error);
            }
        }
        catch (error) {
            throw (error);
        }
        try {
            newFile = fs.createReadStream('VALIDZIPFILE.ZIP');
            formData = { file: newFile };
            response = await new Promise((resolve, reject) => {
                request.post({ url: url, formData: formData }, function (err, httpResponse, body) {
                    if (err) {
                        reject(err);
                    }
                    try {
                        assert.equal(httpResponse.statusCode, 200);
                        assert.equal(JSON.parse(httpResponse.body).success, true);
                        resolve();
                    }
                    catch (error) {
                        resolve(error);
                    }
                })
            })
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.delete("http://" + IP + ":1207/cs/api/projectZip", { params: { fileName: 'VALIDZIPFILE.ZIP' } });
            try {
                assert.equal(response.data.success, true);
                assert.equal(response.status, 200);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
    })
    it("4530- /cs/api/verifyProject should not allow non-zip files", async function () {
        newFile = fs.createReadStream('MYPRG.PRG');
        formData = { file: newFile };
        const url = "http://" + IP + ":1207/cs/api/verifyProject"
        try {
            let response = await new Promise((resolve, reject) => {
                request.post({ url: url, formData: formData }, function (err, httpResponse, body) {
                    if (err) {
                        reject(err);
                    }
                    try {
                        assert.equal(httpResponse.statusCode, 403);
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

    })
})

