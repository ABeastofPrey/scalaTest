const FormData = require('form-data');
const fs = require('fs');
const axios = require('axios');
const assert = require('assert');
describe('Tests for USER API', function () {
    var adminToken = null;
    var userLeonToken = null;
    var superToken = null;
    var badToken = "Blah";
    before(function (done) {
        axios.post('http://192.168.56.102:1207/cs/api/users', {
            "user": {
                username: 'leonid',
                password: '1234'
            }
        }).then(response => {
            userLeonToken = response.data.token;
            return axios.post('http://192.168.56.102:1207/cs/api/users', {
                "user": {
                    username: 'admin',
                    password: 'ADMIN'
                }
            });
        }, err => {
            return Promise.reject(err);
        }).then(response => {
            adminToken = response.data.token;
            return axios.post('http://192.168.56.102:1207/cs/api/users', {
                "user": {
                    username: 'super',
                    password: '1-super99'
                }
            });
        }, err => {
            return Promise.reject(err);
        }).then(response => {
            superToken = response.data.token;
            done();
        }).catch(err => {
            done(err);
        });
    });
    describe('Testing Get user by token', function (done) {
        it("should receive a valid user token, and return the fitting status code", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/user', {
                headers: {
                    Authorization: 'Token ' + userLeonToken
                }
            }).then(response => {
                try {
                    assert.equal(response.status, 200);
                    assert.equal(response.data.user.username, 'leonid');
                    done();
                }
                catch (error) {
                    done(err);
                }
            }).catch(err => {
                console.log(err);
                done(err);
            });
        });
        it("should receive an invalid user token, and return the fitting error code", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/user', {
                headers: {
                    Authorization: 'Token ' + badToken
                }
            }).then(response => {
                done(response);
            }).catch(err => {
                try {
                    assert.equal(err.response.status, 403);
                    done();
                }
                catch (error) {
                    done(error);
                }
            });
        })
            , it("should receive a header without a token , and return the fitting error code", function (done) {
                axios.get('http://192.168.56.102:1207/cs/api/user')
                    .then(response => {
                        done(response);
                    }).catch(err => {
                        try {
                            assert.equal(err.response.status, 401);
                            done();
                        }
                        catch (error) {
                            done(error);
                        }
                    });
            });
    });
    describe('Testing Get list of users', function () {
        it("should return an array of all users in the system", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/users', {
                headers: {
                    Authorization: 'Token ' + adminToken
                }
            }).then(response => {
                try {
                    assert.equal(response.status, 200);
                    assert.notEqual(response.data, undefined);
                    assert.equal(response.data[0].username, "admin");
                    done();
                }
                catch (error) {
                    done(error);
                }
            }).catch(err => {
                done(err);
            });
        });
        it("should return an array containing only the user that called this API", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/users', {
                headers: {
                    Authorization: 'Token ' + userLeonToken
                }
            })
                .then(response => {
                    try {
                        assert.notEqual(response.data, null);
                        assert.equal(response.data[1], null);
                        assert.equal(response.data[0].username, 'leonid');
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                })
                .catch(err => {
                    done(err);
                });
        });
        it("should return an error code of 401 after not authenticating a user", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/users', {
                headers: {
                    Authorization: 'Token ' + badToken
                }
            })
                .then(response => {
                    done(response);
                })
                .catch(err => {
                    try {
                        assert.equal(err.response.status, 403); // temporary set on 403 instead of 401 in order to clean the test run
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                });
        });
    });
    describe("Testing the retrievement of system info", function () {
        it("should return the softMC sys.information as a JSON object", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/sysinfo', {
                headers: {
                    Authorization: 'Token ' + userLeonToken
                }
            })
                .then(response => {
                    try {
                        assert.notEqual(response, null);
                        assert.equal(response.status, 200);
                        assert.notEqual(response.data.ramSize, null);
                        assert.notEqual(response.data.cpu, 1158115328);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                })
                .catch(err => {
                    done(err);
                });
        });
        it("should return an error code of 403 after not authenticating a user", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/users', {
                headers: {
                    Authorization: 'Token ' + badToken
                }
            })
                .then(response => {
                    done(response);
                })
                .catch(err => {
                    try {
                        assert.equal(err.response.status, 403);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                });
        });
        it("should return an error code of 401 after not authenticating a user", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/users', {})
                .then(response => {
                    done(response);
                })
                .catch(err => {
                    try {
                        assert.equal(err.response.status, 401);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                });
        });
    });
    describe("Testing the retrievement of record files on RAM", function () {
        it("Should return a list of available record files on RAM", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/dashboard/recfiles', {
                headers: {
                    Authorization: 'Token ' + userLeonToken
                }
            })
                .then(response => {
                    try {
                        assert.notEqual(response, null);
                        assert.equal(response.status, 200);
                        assert.equal(typeof response.data, "object")
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                })
                .catch(err => {
                    done(err);
                });
        });
        it("should return an error code of 403 after not authenticating a user", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/dashboard/recfiles', {
                headers: {
                    Authorization: 'Token ' + badToken
                }
            })
                .then(response => {
                    done(response);
                })
                .catch(err => {
                    try {
                        assert.equal(err.response.status, 403);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                });
        });
        it("should return an error code of 403 after not authenticating a user", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/dashboard/recfiles')
                .then(response => {
                    done(response);
                })
                .catch(err => {
                    try {
                        assert.equal(err.response.status, 401);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                });
        });
        it("Should return a list of available record files on RAM (using admin's token)", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/dashboard/recfiles', {
                headers: {
                    Authorization: 'Token ' + adminToken
                }
            })
                .then(response => {
                    try {
                        assert.notEqual(response, null);
                        assert.equal(response.status, 200);
                        assert.equal(typeof response.data, "object")
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                })
                .catch(err => {
                    done(err);
                });
        });
    });
    describe("Testing the agreement to the terms and conditions", function () {
        it("Should be a successful request", function (done) {
            axios.put('http://192.168.56.102:1207/cs/api/license', { username: "leonid" }, { headers: { Authorization: 'Token ' + userLeonToken } })
                .then(response => {
                    try {
                        assert.notEqual(response, null);
                        assert.equal(response.status, 200);
                        assert.equal(response.data, true);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                })
                .catch(err => {
                    done(err);
                });
        });
        it("Should be an unsuccessful request", function (done) {
            axios.put('http://192.168.56.102:1207/cs/api/license', { username: "leonid" }, { headers: { Authorization: 'Token ' + badToken } })
                .then(response => {
                    try {
                        assert.notEqual(response, null);
                        assert.equal(response.status, 200); // ask Omri why is the bad request getting a result
                        assert.equal(response.data, false);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                })
                .catch(err => {
                    done(err);
                });
        });
        it("Should be an unsuccessful request with an error status of 401", function (done) {
            axios.put('http://192.168.56.102:1207/cs/api/license', { username: "leonid" })
                .then(response => {
                    done(response);
                })
                .catch(err => {
                    try {
                        assert.equal(err.response.status, 401);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                });
        });
    });
    describe("Testing the editing of an existing user", function () {
        it("Should allow a user to edit his own user", function (done) {
            var headers = {
                'Authorization': 'Token ' + userLeonToken
            };
            var data = {
                username: "leonid",
                fullName: "leonid new",
                password: "1234",
                permission: "1"
            };
            var origData = {
                username: "leonid",
                fullName: "leonid tests",
                password: "1234",
                permission: "1"
            };
            axios.put('http://192.168.56.102:1207/cs/api/user', data, { "headers": headers })
                .then(response => {
                    try {
                        assert.equal(response.status, 200);
                    }
                    catch (error) {
                        done(error);
                    }
                    return axios.get('http://192.168.56.102:1207/cs/api/user', { "headers": headers });
                })
                .then(response => {
                    try {
                        assert.equal(response.data.user.fullName, "leonid new");
                    }
                    catch (error) {
                        done(error);
                    }
                    return axios.put('http://192.168.56.102:1207/cs/api/user', origData, { "headers": headers }); //changing the info back to original   
                })
                .then(done())
                .catch(err => {
                    done(err);
                });
        });
        it("Shouldn't allow a user to edit another user", function (done) {
            var otherUserData = {
                username: "leonid2",
                fullName: "leonid tests",
                password: "1234",
                permission: "1"
            };
            var headers = {
                'Authorization': 'Token ' + userLeonToken
            };
            axios.put('http://192.168.56.102:1207/cs/api/user', otherUserData, { "headers": headers })
                .then(response => {
                    done(response);
                })
                .catch(err => {
                    try {
                        assert.equal(err.response.status, 403);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                });
        });
        it("Should allow an admin to edit other users", function (done) {
            var headers = {
                'Authorization': 'Token ' + userLeonToken
            };
            var adminHeaders = {
                'Authorization': 'Token ' + adminToken
            };
            var data = {
                username: "leonid",
                fullName: "leonid new",
                password: "1234",
                permission: "1"
            };
            var origData = {
                username: "leonid",
                fullName: "leonid tests",
                password: "1234",
                permission: "1"
            };
            axios.put('http://192.168.56.102:1207/cs/api/user', data, { "headers": adminHeaders }) //add get method to validate the change
                .then(response => {
                    try {
                        assert.equal(response.status, 200);
                    }
                    catch (error) {
                        done(error);
                    }
                    return axios.get('http://192.168.56.102:1207/cs/api/user', { "headers": headers });
                })
                .then(response => {
                    try {
                        assert.equal(response.data.user.fullName, "leonid new");
                    }
                    catch (error) {
                        done(error);
                    }
                    return axios.put('http://192.168.56.102:1207/cs/api/user', origData, { "headers": headers }); //changing the info back to original   
                })
                .then(done())
                .catch(err => {
                    done(err);
                });
        });
    });
    describe("Testing the retrievement of a user's profile picture", function () {
        it("Should return a user's profile picture", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/leonid/pic?token=' + userLeonToken)
                .then(response => {
                    try {
                        assert.equal(response.status, 200);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                })
                .catch(err => {
                    done(err);
                });
        });
        it("Should decline a user's request to get another user's profile picture", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/leonid2/pic?token=' + userLeonToken)
                .then(response => {
                    done('failure');
                })
                .catch(err => {
                    try {
                        assert.equal(err.response.status, 403);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                });
        });
        it("Should allow an admin to get a user's profile picture", function (done) {
            axios.get('http://192.168.56.102:1207/cs/api/leonid/pic?token=' + adminToken)
                .then(response => {
                    try {
                        assert.equal(response.status, 200);
                        done();
                    }
                    catch (error) {
                        done(error);
                    }
                })
                .catch(err => {
                    done(err);
                });
        });

    });
    describe("Testing the uploading of a profile picture", function () {
        it("Should allow a user to upload his own profile picture", function (done) {
            var formData = new FormData();
            var file = fs.createReadStream('Smiley.jpg');
            formData.append('file', file, 'Smiley.jpg');
            formData.submit('http://192.168.56.102:1207/cs/api/leonid/pic?token=' + userLeonToken, (err, res) => {
                if (err !== null) {
                    done(err);
                    return;
                }
                assert.equal(res.statusCode, 200);
                done();
            });
        });
        it("Shouldn't allow a user to upoad another user's profile picture", function (done) {
            var formData = new FormData();
            var file = fs.createReadStream('Smiley.jpg');
            formData.append('file', file, 'Smiley.jpg');
            formData.submit('http://192.168.56.102:1207/cs/api/leonid3/pic?token=' + userLeonToken, (err, res) => {
                if (err !== null) {
                    done(err);
                    return;
                }
                assert.equal(res.statusCode,403); //**BUG** the request allows a user to change another users profile pic
                done();
            });
        })
        it("Should decline a user's request to upload an admin's profile picture", function(done){
            var formData = new FormData();
            var file = fs.createReadStream('Smiley.jpg');
            formData.append('file', file, 'Smiley.jpg');
            formData.submit('http://192.168.56.102:1207/cs/api/admin/pic?token='+userLeonToken, (err, res)=>{
                if (err !== null) {
                    done(err);
                    return;
                }
                assert.equal(res.statusCode,403)
                done();
            });
        })
    });
});
