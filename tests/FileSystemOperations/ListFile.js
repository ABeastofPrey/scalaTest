var common = require("../common");
var Client = common.Client;
var IP = common.IP;
var axios = common.axios
var assert = common.assert;
let resetFileSystem= common.resetFileSystem;


describe("4551- List files", function () {
    it("4552-/cs/files should return all files in SSMC", async function () {
        try {
            let response = await resetFileSystem();
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/files");
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, 'MYPRG.PRG');
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
    })
    it("4553-/cs/api/files should return all files AND folders in SSMC (file system structure) ", async function () {
        try {
            let response = await resetFileSystem();
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/files");
            assert.equal(response.data.children[0].path,'MYFOLDER');
            assert.equal(response.data.files[0],'MYPRG.PRG');
            assert.equal(response.status, 200);
        } catch (error) {
            throw (error);
        }
    })

})