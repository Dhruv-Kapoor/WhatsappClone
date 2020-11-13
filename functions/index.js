//'use strict';
//
const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp(functions.config().firebase);

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
exports.sendNotification = functions.database.ref('/messages/{receiverId}/{senderId}/{msgId}')
    .onCreate(async (snapshot, context) => {
    const senderId = context.params.senderId;
    const receiverId = context.params.receiverId;

    let sender;
    let token;
    const senderPromise = admin.firestore().collection("users").doc(senderId).get().then(doc => {
           sender = doc.data();
           return;
    });
    const tokenPromise = admin.firestore().collection("users").doc(receiverId).get().then(doc => {
            token = doc.data().deviceToken;
            return true;
        })
    const r = await Promise.all([senderPromise, tokenPromise]);

    const payload = {
//            notification:{
//                title:sender.phoneNumbe8r,
//                body:snapshot.val().message,
//                icon:sender.thumbImg
//            },
            data:{
                senderUid:senderId,
                title:sender.phoneNumber,
                body:snapshot.val().message,
                icon:sender.thumbImg
            }
          };
    console.log(token);
    const response = await admin.messaging().sendToDevice(token, payload);

    return response;
    })
