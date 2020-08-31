var common = require("../common")
var IP = common.IP;
var axios = common.axios
var assert = common.assert;
let resetFileSystem = common.resetFileSystem;
const fs = common.fs;
const FormData = require('form-data');
const request = require('request');
let logAdmin = common.logAdmin;
let sendSSHCommand = common.sendSSHCommand;



describe("4518-Firmware Update", function () {

    it("4519- API only supports RPM,IPK,ZIP extensions", async function () {
        try {
            let response = await logAdmin();
        } catch (error) {
            throw (error);
        }
        let newFile = fs.createReadStream('MYPRG.PRG');
        let formData = { file: newFile };
        const firmwareUpldUrl = "http://" + IP + ":1207/cs/firmware?token=" + adminToken;
        request.post({ url: firmwareUpldUrl, formData: formData }, function (err, httpResponse, body) {
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
    })
    it("4520- Valid firmware files can be uploaded", async function () {
        try {
            let response = await logAdmin();
        } catch (error) {
            throw (error);
        }
        let newFile = fs.createReadStream('./FileSystemOperations/Wukong.zip'); 
        let formData = { file: newFile };
        const firmwareUpldUrl = "http://" + IP + ":1207/cs/firmware?token=" + adminToken;
        request.post({ url: firmwareUpldUrl, formData: formData }, function (err, httpResponse, body) {
            if (err) {
                throw (err);
            }
            try {
                assert.equal(JSON.parse(httpResponse.body).success, true);
                assert.equal(httpResponse.statusCode, 200);
            }
            catch (error) {
                throw (error)
            }
        })
        try {
            response = await sendSSHCommand("ls /var/home/pkgd.log");
            assert.equal(response.substring(5),"var/home/pkgd.log: No such file or directory" )
        } catch (error) {
            throw (error);
        }



    })
})