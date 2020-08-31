var common = require("./common")
var IP = common.IP;
const WebSocket = require('ws');
const axios = require('axios');
const assert = require('assert');
let logAdmin = common.logAdmin;
let socket;
function send(msg, token) {
    let data = {
        msg: msg,
        cmd_id: 0,
        token: token
    };
    socket.send(JSON.stringify(data));
}

describe("4556- Websockets", function () {
    it("4557- Server should open entry station (a connection to MC) for every websocket connection on port 3010", async function () {
        let timer;
        this.timeout(10000);
        try {
            let response = await logAdmin();
        } catch (error) {
            throw (error);
        }
        try {
            response = await new Promise((resolve, reject) => {
                socket = new WebSocket("ws://" + IP + ":3010");
                let result = null;
                socket.onopen = function (event) {
                    send('?ver', adminToken);
                }
                socket.onmessage = function (event) {
                    result = JSON.parse(event.data);
                    socket.close();
                    clearTimeout(timer);
                    resolve(result);
                }
                timer = setTimeout(() => {
                    try{
                        socket.close();
                    } catch (err) {}
                    if (result === null) {
                        reject(new Error("Didn't get result from websocket"))
                    }
                }, 3000);
            });
            await common.waitTime(3000);
            try {
                assert.equal(response.msg.substring(0, 14), "Version Number");
            } catch (error) {
                throw (error);
            };
        } catch (error) {
            throw (error);
        }
    })
    it("4558- Websocket on port 3010 should only answer requests with a valid token", async function () { //Dont forget to insert check of status code
        this.timeout(10000);
        try {
            response = await new Promise((resolve, reject) => {
                socket = new WebSocket("ws://" + IP + ":3010");
                let result = null;
                socket.onopen = function (event) {
                    send('?ver', "ABCDE");
                }
                socket.onmessage = function (event) {
                    result = JSON.parse(event.data);
                    socket.close();
                    clearTimeout(timer);
                    resolve(result);
                }
                socket.onclose = function (event) {
                    try {
                        assert.equal(event.code, 4001);
                        resolve()
                    } catch (error) {
                        reject(error)
                    }
                }
                timer = setTimeout(() => {
                    try {
                        socket.close();
                        resolve()
                    } catch (err) { resolve() }
                    if (result === null) {
                        reject(new Error("Didn't get result from websocket"))
                    }
                }, 3000);
            });
            await common.waitTime(3000);
            try {
                assert.equal(response.msg, "This request requires a valid token.");
            } catch (error) {
                throw (error);
            };
        }
        catch (error) {
            throw (error);
        }
    })
    it("4559- Server should close websocket on port 3010 if there are no available entry stations", async function () {
        this.timeout(20000);
        let firstSocket, secondSocket;
        try {
            response = await new Promise((resolve,reject) => {
                firstSocket = new WebSocket("ws://" + IP + ":3010");
                firstSocket.onopen = function (event) {
                }
                firstSocket.onclose = function (event) {
                }
                setTimeout(()=>{
                    secondSocket = new WebSocket("ws://" + IP + ":3010");
                    let result = null;
                    secondSocket.onopen = function (event) {
                        console.log('socket 2 opened');
                    }
                    secondSocket.onclose = function (event) {
                        try {
                            firstSocket.close();
                            clearTimeout(timer);
                            assert.equal(event.code, 4003);
                            resolve();
                        } catch (error) {
                            reject(error)
                        }
                    }
                    timer = setTimeout(() => {
                        try {
                            firstSocket.close();
                            secondSocket.close();
                            reject(new Error("Socket 2 wasn't closed!"));
                        } catch (err) { }
                    }, 6000);
                },2000);
            });
        }
        catch (error) {
            throw (error);
        }
    })
})