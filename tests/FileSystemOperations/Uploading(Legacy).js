var common = require("../common")
var IP = common.IP;
var axios = common.axios
var assert = common.assert;
let resetFileSystem = common.resetFileSystem;
let logAdmin = common.logAdmin;
const FormData = require('form-data');
const request = require('request');
const fs = common.fs;



function lookForUpload(files) {
    files = files.split(',');
    if (files[0] == 'UPLOAD.PRG' || files[1] == 'UPLOAD.PRG') {
        return true;
    }
    return false;
}


describe("4515- Uploading", function () {
    const upldUrl = "http://" + IP + ":1207/cs/upload";
    const overWriteUpldUrl = "http://" + IP + ":1207/cs/upload/overwrite";


    it("4516- Everyone should be able to upload a file to SSMC", async function () {
        try {
            let response = await resetFileSystem();
        }
        catch (error) {
            throw (error);
        }
        let newFile = fs.createReadStream('./FileSystemOperations/UPLOAD.PRG');
        let formData = { file: newFile };
        await new Promise((resolve, reject) => {
            request.post({ url: upldUrl, formData: formData }, function (err, httpResponse, body) {
                if (err) {
                    reject(err);
                }
                try {
                    assert.equal(httpResponse.statusCode, 200);
                    assert.equal(JSON.parse(httpResponse.body).success, true);
                    resolve();
                }
                catch (error) {
                    reject(error)
                }
            });
        });
        try {
            response = await axios.get("http://" + IP + ":1207/cs/files");
            assert.equal(lookForUpload(response.data), true);
        } catch (error) {
            throw (error);
        }
        try {
            newFile = fs.createReadStream('./FileSystemOperations/UPLOAD.PRG');
            formData = { file: newFile };
            response = await new Promise((resolve, reject) => {
                request.post({ url: upldUrl, formData: formData }, function (err, httpResponse, body) {
                    if (err) {
                        reject(err);
                    }
                    try {
                        assert.equal(JSON.parse(httpResponse.body).err, -1);
                        assert.equal(httpResponse.statusCode, 200);
                        assert.equal(JSON.parse(httpResponse.body).success, false);
                        resolve();
                    }
                    catch (error) {
                        reject(error)
                    }
                })
            })
        }
        catch (error) {
            throw (error)
        }
        try {
            newFile = fs.createReadStream('./FileSystemOperations/UPLOAD.PRG');
            formData = { file: newFile };
            response = await new Promise((resolve, reject) => {
                request.post({ url: overWriteUpldUrl, formData: formData }, function (err, httpResponse, body) {
                    if (err) {
                        reject(err);
                    }
                    try {
                        assert.equal(httpResponse.statusCode, 200);
                        assert.equal(JSON.parse(httpResponse.body).success, true);
                        resolve();
                    }
                    catch (error) {
                        reject(error)
                    }
                })

            })
        }
        catch (error) {
            throw (error);
        }
    })
    it("4517- No one can upload a forbidden extension", async function () {
        try {
            newFile = fs.createReadStream('./FileSystemOperations/1.JPG');
            formData = { file: newFile };
            let response = await request.post({ url: upldUrl, formData: formData }, function (err, httpResponse, body) {
                if (err) {
                    throw (err);
                }
                try {
                    assert.equal(httpResponse.statusCode, 403);
                }
                catch (error) {
                    throw (error)
                }
            })
        }
        catch (error) {
            throw (error);
        }
    })
})