var common = require("../common")
var IP = common.IP;
var axios = common.axios
var assert = common.assert;
let resetFileSystem = common.resetFileSystem;
let logAdmin = common.logAdmin;



describe("4521- Deleting", function () {
    it("4522- API should delete files and empty folders from the controller", async function () {
        try {
            let response = await logAdmin();
        } catch (error) {
            throw(error);
        }
        try {
             response = await resetFileSystem();
        } catch (error) {
            throw (error);
        } try {
            response = await axios.post("http://" + IP + ":1207/cs/api/move", {
                target: "MYFOLDER",
                files: "MYPRG.PRG"
            });
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.delete("http://" + IP + ":1207/cs/file/MYFOLDER?token=" + adminToken, {
                headers: {
                    Authorization: 'Token ' + adminToken
                }
            });
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, false);
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.delete("http://" + IP + ":1207/cs/file/MYFOLDER$$MYPRG.PRG?token=" + adminToken, { //PROBLEM
                headers: {
                    Authorization: 'Token ' + adminToken
                }
            });
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, true); 
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            console.log(error)
            throw (error);
        }
        try {
            response = await axios.delete("http://" + IP + ":1207/cs/file/MYFOLDER?token=" + adminToken, {
                headers: {
                    Authorization: 'Token ' + adminToken
                }
            });
            try {
                assert.equal(response.status, 200);
                assert.equal(response.data, true);
            } catch (error) {
                throw (error);
            }
        }
        catch (error) {
            throw (error);
        }

    })

})
