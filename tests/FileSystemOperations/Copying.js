
var common = require("../common");
let resetFileSystem = common.resetFileSystem;
let logAdmin = common.logAdmin;
const IP = common.IP;
var axios = common.axios
var assert = common.assert;
let logProgrammer = common.logProgrammer;
let logViewer = common.logViewer;
let logOperator = common.logOperator;

async function performCopying(from, to, token) {
    return await axios.post("http://" + IP + ":1207/cs/api/copy", {
        from: from,
        to: to
    }, {
        headers: {
            Authorization: 'Token ' + token
        }
    });
}

describe("4476 - Copying ", function () {
    it("4478- Admins should be able to copy files and folders", async function () {
        try {
            let response = await logAdmin();
            response = await resetFileSystem();
        } catch (error) {
            throw (error);
        } try {
            response = await performCopying("MYPRG.PRG", "MYFOLDER/MYPRG", adminToken)
            assert.equal(response.status, 200);
            //  assert.equal(response.data, true); DONT FORGET TO REMOVE 
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/files");
            assert.equal(response.data.children[0].files[0], "MYPRG");
        } catch (error) {
            throw (error);
        }
        try {
            response = await performCopying("MYFOLDER", "MYFOLDER2", adminToken);
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/files");
            assert.equal(response.data.children[1].path, "MYFOLDER2");
            assert.equal(response.data.children[1].files[0], "MYPRG");
        } catch (error) {
            throw (error);
        }
    })
    it("4480- Non-admins should not be able to copy anything", async function () {
        try {
            let response = await logProgrammer();
            response = await logOperator();
            response = await logViewer();
            response = await resetFileSystem();
        } catch (error) {
            throw (error);
        }
        try {
            response = await performCopying("MYPRG.PRG", "MYPRG2.PRG", programmerToken);
            return Promise.reject(new Error("A request to copy with a programmer permission shouldn't be successful, the response status is: " + response.status));

        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await performCopying("MYPRG.PRG", "MYPRG2.PRG", operatorToken);
            return Promise.reject(new Error("A request to copy with an operator permission shouldn't be successful, the response status is: " + response.status));

        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await performCopying("MYPRG.PRG", "MYPRG2.PRG", viewerToken);
            return Promise.reject(new Error("A request to copy with a viewer permission shouldn't be successful, the response status is: " + response.status));

        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }
        try {
            response = await performCopying("MYPRG.PRG", "MYPRG2", programmerToken);
            response = await axios.post("http://" + IP + ":1207/cs/api/copy", {
                from: "MYPRG.PRG",
                to: "MYPRG2.PRG"
            });
            return Promise.reject(new Error("A request to copy with no permission shouldn't be successful, the response status is: " + response.status));

        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }

    })
})