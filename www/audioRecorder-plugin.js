

    //
    //  audioRecorder-plugin.js
    //  AudioRecorder Plugin
    //
    //  Created by acr on 02/03/2014.
    //  Copyright (c) 2014 University of Southampton. All rights reserved.
    //

    var audioRecorder = exports;

    var fns = ['initialiseAudioRecorder', 'startAudioRecorder', 'stopAudioRecorder', 
               'startWhiteNoise', 'stopWhiteNoise', 
               'startHeterodyne', 'stopHeterodyne', 
               'getAmplitude', 'getScaledAmplitude', 'getFrequencies', 'getScaledFrequencies', 'getFrequencyColours', 
               'captureRecording', 'clearBuffers'];

    // Add functions for each of the plugin callbacks we want to expose

    for (var i = 0; i < fns.length; i++) {

        // Wrap in a closure so that we lock in the value of fnName

        (function () {

            var fnName = fns[i];

            audioRecorder[fnName] = function (win, fail) {

                win = win || function () {};
                fail = fail || function () {};

                cordova.exec(win, fail, "AudioRecorder", fnName, [null]);

            };

        })();
    }

    audioRecorder.setHeterodyneFrequency = function (frequency, win, fail) {

        win = win || function () {};
        fail = fail || function () {};

        frequency = frequency || 15000;

        cordova.exec(win, fail, "AudioRecorder", "setHeterodyneFrequency", [frequency]);

    }

    audioRecorder.writeSonogram = function (width, height, duration, win, fail) {

        win = win || function () {};
        fail = fail || function () {};

        width = width || 320;
        height = height || 120;
        duration = duration || 30;

        cordova.exec(win, fail, "AudioRecorder", "writeSonogram", [width, height, duration]);

    }

    audioRecorder.writeRecording = function (duration, win, fail) {

        win = win || function () {};
        fail = fail || function () {};

        duration = duration || 30;

        cordova.exec(win, fail, "AudioRecorder", "writeRecording", [duration]);

    }

