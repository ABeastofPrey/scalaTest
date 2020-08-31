var common = require("../common")
var axios = common.axios
var IP = common.IP;
var assert = common.assert;
var fs = common.fs;
var FormData = common.FormData;
var request = common.request;
var Client = common.Client;
let getAdminToken = common.signAdmin;
let getSuperToken = common.signSuper;
let signupUsers = common.signupUsers;
let getSimpleTokens = common.getSimpleTokens;
let logAdmin = common.logAdmin;
let logSuper = common.logSuper;
let logProgrammer = common.logProgrammer;

function log(msg){
    const time = new Date().getTime();
    console.log(time + '\t' + msg);
};


describe("4494-Profile Pictures", function () {
    it("4494-Admins should be able to get the pictures of all users", async function () {
      this.timeout(10000);
      try {
        response = await logAdmin();
      } catch (error) {
        throw (error);
      }
      try {
        response = await axios.get("http://" + IP + ":1207/cs/api/PROGRAMMER/pic?token=" + adminToken);
        try {
          assert.equal(response.status, 200);
        } catch (error) {
          throw (error);
        }
      } catch (error) {
        throw (error);
      }
      try {
        response = await logSuper();
      } catch (error) {
        throw (error);
      }
      try {
        response = await axios.get("http://" + IP + ":1207/cs/api/admin/pic?token=" + superToken);
        try {
          assert.equal(response.status, 200);
        } catch (error) {
          throw (error)
        }
      } catch (err) {
        throw (err);
      }
    });


    it("4496- Non admin users should only get their own photo", async function () {
        this.timeout(10000);
        try {
            response = await logProgrammer();
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/PROGRAMMER/pic?token=" + programmerToken);
            try {
                assert.equal(response.status, 200)
            } catch (error) {
                throw (error);
            }
        } catch (error) {
            throw (error);
        }
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/OPERATOR/pic?token=" + programmerToken);
            throw (new Error("A programmer shouldn't be able to get an operator's profile picture  "), response.status)
        }
        catch (error) {
            try {
                assert.equal(error.response.status, 403);
            } catch (error) {
                throw (error);
            }
        }
    })
    it("4497- Non-users should not be able to get any profile picture", async function () {
        try {
            response = await axios.get("http://" + IP + ":1207/cs/api/OPERATOR/pic");
            throw (new Error("A request lacking a valid token shouldn't be successful "), response.status)
        } catch (error) {
            try {
                assert.equal(error.response.status, 401);
            } catch (error) {
                throw (error);
            }
        }

    })
    it("4499- Users should be able to UPLOAD their own profile picture", function (done) {
      var formData = new FormData();
      var file = fs.createReadStream('Smiley.jpg');
      formData.append('file', file, 'Smiley.jpg');
      formData.submit({
        host: IP,
        port: 1207,
        path: '/cs/api/PROGRAMMER/pic',
        headers: {
          'Authorization': 'Token ' + programmerToken
        }
      }, (err,res) => {
        if (err) {
          done(err);
          return;
        }
        assert.equal(res.statusCode, 200);
        done();
      });
    });
    it("4500-Users should not be able to upload someone else's profile pic", function (done) {
        var formData = new FormData();
        var file = fs.createReadStream('Smiley.jpg');
        formData.append('file', file, 'Smiley.jpg');
        formData.submit({
          host: IP,
          port: 1207,
          path: '/cs/api/OPERATOR/pic',
          headers: {
            'Authorization': 'Token ' + programmerToken
          }
        }, (err,res) => {
            if (err) {
                done(err);
                return;
            }
            assert.equal(res.statusCode, 403); //**BUG** the request allows a user to change another users profile pic
            done();
        })
    })
    it("4501- Admin/Super users should NOT be able to change their profile pic", function (done) {
        var formData = new FormData();
        var file = fs.createReadStream('Smiley.jpg');
        formData.append('file', file, 'Smiley.jpg');
        formData.submit({
          host: IP,
          port: 1207,
          path: '/cs/api/admin/pic',
          headers: {
            'Authorization': 'Token ' + adminToken
          }
        }, (err,res) => {
            if (err) {
                done(err);
                return;
            }
            try {
                assert.equal(res.statusCode, 403)
            } catch (error) {
                done(error);
                return;
            }
        })
        formData.submit({
          host: IP,
          port: 1207,
          path: '/cs/api/super/pic',
          headers: {
            'Authorization': 'Token ' + superToken
          }
        }, (err,res) => {
            if (err) {
                done(err);
                return;
            }
            try {
                assert.equal(res.statusCode, 403)
                done();

            } catch (error) {
                done(error);
                return;
            }
        })
    })
})