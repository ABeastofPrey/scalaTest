
    const axios = require('axios');
    const assert = require('assert');
    describe('Tests for ADMIN API', function () {
        var adminToken = null;
        var userToken = null;
        var superToken = null;
        var badToken = "Blah";
        before(function (done) {
            axios.post('http://192.168.56.102:1207/cs/api/users', {
                "user": {
                    username: 'leonid',
                    password: '1234'
                }
            }).then(response => {
                userToken = response.data.token;
                return axios.post('http://192.168.56.102:1207/cs/api/users', {
                    "user": {
                        username: 'admin',
                        password: 'ADMIN'
                    }
                });
            }).catch(err => {
                done(err);
            }).then(response => {
                adminToken = response.data.token;
                return axios.post('http://192.168.56.102:1207/cs/api/users', {
                    "user": {
                        username: 'super',
                        password: '1-super99'
                    }
                });
            }).catch(err => {
                done(err);
            }).then(response => {
                superToken = response.data.token;
                done();
            }).catch(err => {
                done(err);
            });
        });
        // describe("Testing the restorement of the webserver to factory state", function () {
        //     it("Should reset the webserver to factory settings", function (done) {
        //         axios.get('http://192.168.56.102:1207/cs/api/factoryRestore', {
        //             headers: {
        //                 Authorization: 'Token ' + adminToken
        //             }
        //         })
        //             .then(response => {
        //                 try {
        //                     assert.equal(response.data,true)
        //                     done();
        //                 }
        //                 catch (error) {
        //                     done(error);
        //                 }
        //             })
        //             .catch(err => {
        //                 done(err);
        //             });
        //     });
        // });
        describe("Testing the retrievement of the server's log database", function () {
            it("should return the server's log database", function (done) {
                axios.get('http://192.168.56.102:1207/cs/api/log', {
                    headers: {
                        Authorization: 'Token ' + adminToken
                    }
                })
                    .then(response => {
                        try {
                            //optional data to be checked after consulting with Omri
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
            it("should return an error code of 403 after not authenticating a user", function (done) {
                axios.get('http://192.168.56.102:1207/cs/api/log', {
                    headers: {
                        Authorization: 'Token ' + userToken
                    }
                })
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
            it("should return an error code of 401", function (done) {
                axios.get('http://192.168.56.102:1207/cs/api/log')
                    .then(response => {
                        done('failure');
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
    });
