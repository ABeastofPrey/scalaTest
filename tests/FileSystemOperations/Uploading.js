var common = require("../common")
var IP = common.IP;
var axios = common.axios
var assert = common.assert;
let resetFileSystem = common.resetFileSystem;
let logAdmin = common.logAdmin;
const FormData = require('form-data');
const request = require('request');
const fs = common.fs;
let logProgrammer = common.logProgrammer;
let logOperator = common.logOperator;
let logViewer = common.logViewer;
let logSuper = common.logSuper;



describe("4510- Uploading", function () {
    const upldUrlNoToken = "http://" + IP + ":1207/cs/api/upload";
    const upldUrlBadToken = "http://" + IP + ":1207/cs/api/upload?token=ABCDE"

    it("4511- Non-authenticated users CAN'T use this API", async function () {
        try {
            newFile = fs.createReadStream('MYPRG.PRG');
            formData = { file: newFile };
            let response = await request.post({ url: upldUrlNoToken, formData: formData }, function (err, httpResponse, body) {
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
        try {
            newFile = fs.createReadStream('MYPRG.PRG');
            formData = { file: newFile };
            let response = await request.post({ url: upldUrlBadToken, formData: formData }, function (err, httpResponse, body) {
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
    it("4512- Authenticated users should be able to upload a VALID file to a path", async function () {
        this.timeout(10000);
        try {
            let response = await resetFileSystem();
            response = await logAdmin();
            response = await logProgrammer();
            response = await logOperator();
            response = await logViewer();
            response = await logSuper();
        } catch (error) {
            throw (error);
        }
        try {
            newFile = fs.createReadStream('./FileSystemOperations/1.JPG');
            formData = { file: newFile };
            let response = await request.post({
                url: "http://" + IP + ":1207/cs/api/upload?path=MYFOLDER/&overwrite=false&token=" + adminToken,
                formData: formData,
                headers: {
                    Authorization: 'Token ' + adminToken
                }
            }
                , function (err, httpResponse, body) {
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
        try {
            newFile = fs.createReadStream('./FileSystemOperations/UPLOAD.PRG');
            formData = { file: newFile };
            response = await new Promise((resolve, reject) => {
                request.post({
                    url: "http://" + IP + ":1207/cs/api/upload?path=MYFOLDER/&overwrite=false&token=" + adminToken,
                    formData: formData,
                    headers: {
                        Authorization: 'Token ' + adminToken
                    }
                }
                    , function (err, httpResponse, body) {
                        if (err) {
                            reject(err);
                        }
                        try {
                            assert.equal(httpResponse.statusCode, 200);
                            assert.equal(JSON.parse(httpResponse.body).success, true);
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
            response = await axios.get("http://" + IP + ":1207/cs/api/files");
            assert.equal(response.data.children[0].files[0], "UPLOAD.PRG");
        } catch (error) {
            throw (error);
        }
        try {
            newFile = fs.createReadStream('./FileSystemOperations/UPLOAD.PRG');
            formData = { file: newFile };
            response = await new Promise((resolve, reject) => {
                request.post({
                    url: "http://" + IP + ":1207/cs/api/upload?path=MYFOLDER/&overwrite=false&token=" + adminToken,
                    formData: formData,
                    headers: {
                        Authorization: 'Token ' + adminToken
                    }
                }
                    , function (err, httpResponse, body) {
                        if (err) {
                            reject(err);
                        }
                        try {
                            assert.equal(httpResponse.statusCode, 200);
                            assert.equal(JSON.parse(httpResponse.body).success, false);
                            assert.equal(JSON.parse(httpResponse.body).err, -2);
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
            newFile = fs.createReadStream('./FileSystemOperations/UPLOAD.PRG');
            formData = { file: newFile };
            response = await new Promise((resolve, reject) => {
                request.post({
                    url: "http://" + IP + ":1207/cs/api/upload?path=MYFOLDER/&overwrite=true&token=" + adminToken,
                    formData: formData,
                    headers: {
                        Authorization: 'Token ' + adminToken
                    }
                }
                    , function (err, httpResponse, body) {
                        if (err) {
                            reject(err);
                        }
                        try {
                            assert.equal(httpResponse.statusCode, 200);
                            assert.equal(JSON.parse(httpResponse.body).success, true);
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
            response = await axios.get("http://" + IP + ":1207/cs/api/files");
            assert.equal(response.data.children[0].files[0], "UPLOAD.PRG");
        } catch (error) {
            throw (error);
        }
    })
    it("4513- ADMINS should be able to UPLOAD FWCONFIG", async function () {
        this.timeout(10000);
        try {
            response = await logAdmin();
            response = await logProgrammer();
            response = await logOperator();
            response = await logViewer();
            response = await logSuper();
        } catch (error) {
            throw (error);
        }
        try {
            newFile = fs.createReadStream('./FileSystemOperations/FWCONFIG');
            formData = { file: newFile };
            response = await new Promise((resolve, reject) => {
                request.post({
                    url: "http://" + IP + ":1207/cs/api/upload?path=&token=" + adminToken,
                    formData: formData,
                    headers: {
                        Authorization: 'Token ' + adminToken
                    }
                }
                    , function (err, httpResponse, body) {
                        if (err) {
                            reject(err);
                        }
                        try {
                            assert.equal(httpResponse.statusCode, 200);
                            assert.equal(JSON.parse(httpResponse.body).success, true);
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
            newFile = fs.createReadStream('./FileSystemOperations/FWCONFIG');
            formData = { file: newFile };
            response = await new Promise((resolve, reject) => {
                request.post({
                    url: "http://" + IP + ":1207/cs/api/upload?path=&token=" + superToken,
                    formData: formData,
                    headers: {
                        Authorization: 'Token ' + superToken
                    }
                }
                    , function (err, httpResponse, body) {
                        if (err) {
                            reject(err);
                        }
                        try {
                            assert.equal(httpResponse.statusCode, 200);
                            assert.equal(JSON.parse(httpResponse.body).success, true);
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
    it("4514- Non-admin users CAN'T upload FWCONFIG", async function () {
        try {
            let response = logProgrammer();
        } catch (error) {
            throw (error);
        }
        try {
            newFile = fs.createReadStream('./FileSystemOperations/FWCONFIG');
            formData = { file: newFile };
            response = await new Promise((resolve, reject) => {
                request.post({
                    url: "http://" + IP + ":1207/cs/api/upload?path=&token=" + programmerToken,
                    formData: formData,
                    headers: {
                        Authorization: 'Token ' + programmerToken
                    }
                }
                    , function (err, httpResponse, body) {
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
