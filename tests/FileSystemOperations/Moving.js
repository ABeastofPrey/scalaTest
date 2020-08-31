var common = require("../common")
var IP = common.IP;
var axios = common.axios
var assert = common.assert;
let resetFileSystem = common.resetFileSystem;

describe("4541- Moving", function () {
    it("4542- /cs/api/move should be able to move folders and files", async function () {
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
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, true);
                response = await axios.get("http://" + IP + ":1207/cs/api/files");
                assert.equal(response.data.children[0].files[0], 'MYPRG.PRG');
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.post("http://" + IP + ":1207/cs/api/folder", { path: "TEST" });
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, true)
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.post("http://" + IP + ":1207/cs/api/move", {
                target: "TEST",
                files: "MYFOLDER"
            });
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, true);
                response = await axios.get("http://" + IP + ":1207/cs/api/files");
                assert.equal(response.data.children[0].children[0].path, 'MYFOLDER');
                assert.equal(response.data.children[0].children[0].files[0], 'MYPRG.PRG');
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);

        }

    })
})