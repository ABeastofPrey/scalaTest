const axios = require('axios');
const assert = require('assert');
const fs = require('fs');
const FormData = require('form-data');
const request = require('request');
const IP = "10.4.20.86";
const Client = require('ssh2').Client;
global.programmerToken = null;
global.operatorToken = null;
global.viewerToken = null;
global.adminToken = null;
global.superToken = null;

//MUST ADD METHODS OF GETTING TOKENS OF ADMIN, SUPER AND OTHER USERS(3 DIFFERENT METHODS)
function DeleteUser(userToDelete, deletingUserToken) {
  return axios.delete("http://" + IP + ":1207/cs/api/user/" + userToDelete, {
    headers: {
      Authorization: 'Token ' + deletingUserToken
    }
  });
}
function sendSSHCommand(cmd) {
  const cmdLines = cmd.split('\n').length;
  return new Promise((resolve, reject) => {
      var conn = new Client();
      conn.on('error', e => {
          reject(e);
      });
      conn.on('ready', function () {
          let result = '';
          conn.shell(function (err, stream) {
              if (err) throw err;
              stream.on('close', function () {
                  conn.end();
                  let validData = result.split('\r\n')
                  resolve(validData.slice(cmdLines + 3, validData.length - 3).join('\n'))
              }).on('data', function (data) {
                  result += data;
              });
              stream.end(cmd + '\nexit\n');
          });
      }).connect({
          host: IP,
          port: 22,
          username: 'root',
          password: '1-login11'
      });
  });
}
function resetFileSystem() {
  return new Promise((resolve, reject) => {
    var conn = new Client();
    conn.on('ready', function () {
      let result = '';
      conn.shell(function (err, stream) {
        if (err) reject(err);
        stream.on('close', function () {
          conn.end();
          resolve();
        }).on('data', function (data) {
          result += data;
        });
        const cmd1 = 'rm -fr /FFS0/SSMC\n';
        const cmd2 = 'mkdir /FFS0/SSMC\n';
        const cmd3 = 'mkdir /FFS0/SSMC/MYFOLDER\n';
        const cmd4 = 'touch /FFS0/SSMC/MYPRG.PRG\n';
        const cmd5 = 'echo hello > /FFS0/SSMC/MYPRG.PRG\n';
        const cmd6 = 'chown -R mc:mc /FFS0/SSMC\n';
        stream.end(cmd1 + cmd2 + cmd3 + cmd4 + cmd5 + cmd6 + 'exit\n');
      });
    }).connect({
      host: IP,
      port: 22,
      username: 'root',
      password: '1-login11'
    });
  });
}


function logAdmin() {
  const url = 'http://' + IP + ':1207/cs/api/users';
  return axios.post(url, {
    user: {
      username: "admin",
      password: "ADMIN"
    }
  })
    .then(response => {
      adminToken = response.data.token;
      return response;
    })
    .catch(error => {
      return Promise.reject(error);
    });
}
function logSuper() {
  return axios.post('http://' + IP + ':1207/cs/api/users', {
    user: {
      username: "super",
      password: "1-super99"
    }
  })
    .then(response => {
      superToken = response.data.token;
      return response;
    })
    .catch(error => {
      return Promise.reject(error);
    });
}
function logProgrammer() {
  return axios.post('http://' + IP + ':1207/cs/api/users', {
    user: {
      username: "PROGRAMMER",
      password: "1234"
    }
  })
    .then(response => {
      programmerToken = response.data.token;
    })
    .catch(error => {
      return Promise.reject(error);
    });
}
function logOperator() {
  return axios.post('http://' + IP + ':1207/cs/api/users', {
    user: {
      username: "OPERATOR",
      password: "1234"
    }
  })
    .then(response => {
      operatorToken = response.data.token;
    })
    .catch(error => {
      return Promise.reject(error);
    });
}
function logViewer() {
  return axios.post('http://' + IP + ':1207/cs/api/users', {
    user: {
      username: "VIEWER",
      password: "1234"
    }
  })
    .then(response => {
      viewerToken = response.data.token;
    })
    .catch(error => {
      return Promise.reject(error);
    });
}

function signAdmin() {
  return axios.post('http://' + IP + ':1207/cs/api/users', {
    "user": {
      username: 'admin',
      password: 'ADMIN'
    }
  })
    .then(response => {
      adminToken = response.data.token;
    })
    .catch(error => {
      return Promise.reject(error);
    });
}
function signSuper() {
  return axios.post('http://' + IP + ':1207/cs/api/users', {
    "user": {
      username: 'super',
      password: '1-super99'
    }
  })
    .then(response => {
      superToken = response.data.token;
    })
    .catch(error => {
      return Promise.reject(error);
    });
}

function signOperator(adminToken) {
  return axios.post('http://' + IP + ':1207/cs/api/signup', {
    username: "OPERATOR",
    password: "1234",
    fullName: "OPERATOR OPERATOR",
    permission: "2"
  }, {
    headers: {
      Authorization: 'Token ' + adminToken
    }
  })
}

function signViewer(adminToken) {
  return axios.post('http://' + IP + ':1207/cs/api/signup', {
    username: "VIEWER",
    password: "1234",
    fullName: "VIEWER VIEWER",
    permission: "3"
  }, {
    headers: {
      Authorization: 'Token ' + adminToken
    }
  })
}



function signProgrammer(adminToken) {
  return axios.post('http://' + IP + ':1207/cs/api/signup', {
    username: "PROGRAMMER",
    password: "1234",
    fullName: "PROGRAMMER PROGRAMMER",
    permission: "1"
  }, {
    headers: {
      Authorization: 'Token ' + adminToken
    }
  })
}

function signupUsers(adminToken) { // BAD FUNCTION !!!
  return axios.post('http://' + IP + ':1207/cs/api/signup', {
    username: "PROGRAMMER",
    password: "1234",
    fullName: "PROGRAMMER PROGRAMMER",
    permission: "1"
  }, {
    headers: {
      Authorization: 'Token ' + adminToken
    }
  })
    .then(response => {
      try {
        assert.equal(response.data, true)
        assert.equal(response.status, 200);
      } catch (error) {
        done(error)
        return Promise.reject(error);
      }
      return axios.post('http://' + IP + ':1207/cs/api/signup', {
        username: "OPERATOR",
        password: "1234",
        fullName: "OPERATOR OPERATOR",
        permission: "2"
      }, {
        headers: {
          Authorization: 'Token ' + adminToken
        }
      })
        .then(response => {
          try {
            assert.equal(response.data, true)
            assert(response.status, 200)

          } catch (error) {
            done(error)
            return Promise.reject(error);

          }
          return axios.post('http://' + IP + ':1207/cs/api/signup', {
            username: "VIEWER",
            password: "1234",
            fullName: "VIEWER VIEWER",
            permission: "3"
          }, {
            headers: {
              Authorization: 'Token ' + adminToken
            }
          })
        })
        .catch(error => {
          return Promise.reject();
        })
        .then(response => {
          try {
            assert.equal(response.data, true)
            assert.equal(response.status, 200)
          } catch (error) {
            done(error);
            return Promise.reject();
          }
        })
    })
}

function waitTime(ms) {
  return new Promise(resolve=>{
    setTimeout(()=>{
      resolve();
    },ms);
  });
}

exports.axios = axios;
exports.assert = assert;
exports.fs = fs;
exports.FormData = FormData;
exports.request = request;
exports.IP = IP;
exports.Client = require('ssh2').Client;
exports.getSuperToken = signSuper;
exports.getAdminToken = signAdmin;
exports.signupUsers = signupUsers;
exports.logAdmin = logAdmin;
exports.logSuper = logSuper;
exports.logProgrammer = logProgrammer;
exports.signProgrammer = signProgrammer;
exports.signOperator = signOperator;
exports.logOperator = logOperator;
exports.signViewer = signViewer;
exports.logViewer = logViewer;
exports.DeleteUser = DeleteUser;
exports.resetFileSystem = resetFileSystem;
exports.sendSSHCommand = sendSSHCommand;
exports.waitTime = waitTime;
// exports.getSimpleTokens = getSimpleTokens;