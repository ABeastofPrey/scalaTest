var common = require("../common")
var IP = common.IP;
var axios = common.axios
var assert = common.assert;
let resetFileSystem = common.resetFileSystem;


describe("4539- Folders", function () {
    it("4540- /cs/api/folder should try to create a new folder", async function () {
        try {
            let response = await resetFileSystem();
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.post("http://" + IP + ":1207/cs/api/folder", { path: "TEST" });
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, true);
            } catch (error) {
                throw (error)
            }
        } catch (error) {
            throw (error)
        }
        try {
            response = await axios.post("http://" + IP + ":1207/cs/api/folder", { path: "TEST" });
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, false);
            } catch (error) {
                throw (error)
            }
        } catch (error) {
            throw (error);
        }
    })

})