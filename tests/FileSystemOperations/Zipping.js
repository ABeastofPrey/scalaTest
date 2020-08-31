
var common = require("../common");
const http = require('http');
var fs = common.fs;
var yauzl = require("yauzl");
var Client = common.Client;
var IP = common.IP;
var axios = common.axios
var assert = common.assert;
let resetFileSystem = common.resetFileSystem;

function getMCZip() {
    return new Promise((resolve, reject) => {
        http.get("http://" + IP + ":1207/cs/api/zipFile", response => {
            const file = fs.createWriteStream("MCFiles.ZIP");
            response.pipe(file);
            file.on('finish', function () {
                file.close(function () {
                    yauzl.open("MCFiles.ZIP", { lazyEntries: true }, function (err, zipfile) {
                        var SSMC = [];
                        if (err) {
                            return reject(err);
                        }
                        zipfile.readEntry();
                        zipfile.on("entry", function (entry) {
                            SSMC.push(entry.fileName);
                            zipfile.readEntry();
                        });
                        zipfile.once("end", function () {
                            zipfile.close();
                            resolve(SSMC);
                        });
                    });
                });
            });
        });
    });
}

describe("4525-Zipping", function () {
    it("4527- API Should be able to create a ZIP file of any folder", async function () {
        try {
            let response = await resetFileSystem();
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.post("http://" + IP + ":1207/cs/mczip");
            assert.equal(response.status, 200);
            assert.equal(response.data, true);
        } catch (error) {
            throw (error);
        }
        try {
            response = await getMCZip();
            try {
                response = response.sort();
                assert.equal(response[0], 'MYFOLDER/');
                assert.equal(response[1], 'MYPRG.PRG');
            } catch (error) {
                throw (error);
            }

        } catch (error) {
            throw (error);
        }
    })
})

