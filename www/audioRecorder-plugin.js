
//
//  audioRecorder-plugin.js
//  AudioRecorder Plugin
//
//  Created by acr on 02/03/2014.
//  Copyright (c) 2014 University of Southampton. All rights reserved.
//

var audioRecorder = module.exports;

var generateDelayedCallback = function (callback) {

	var delayedCallback = function(data) {
		setTimeout(function() {
			callback(data);
		}, 10);
	};

	return delayedCallback;

};

var fns = ['initialiseAudioRecorder', 'startAudioRecorder', 'stopAudioRecorder', 'startWhiteNoise', 'stopWhiteNoise', 'startHeterodyne', 'stopHeterodyne', 'getAmplitude', 'getFrequencies', 'captureRecording'];

// Add functions for each of the plugin callbacks we want to expose

for(var i=0; i<fns.length; i++) {

	// Wrap in a closure so that we lock in the value of fnName
	
	(function() {

		var fnName = fns[i];

		audioRecorder.prototype[fnName] = function(win, fail) {

			win = win || function() {};
			fail = fail || function() {};
			
			cordova.exec(generateDelayedCallback(win), fail, "CicadaDetector", fnName, [null]);

		};

	})();
}

audioRecorder.prototype.setHeterodyneFrequency = function(win, fail, frequency) {

	win = win || function() {};
	fail = fail || function() {};

	frequency = frequency || 15000;

	cordova.exec(generateDelayedCallback(win), fail, "AudioRecorder", "setHeterodyneFrequency", [frequency]);

}

audioRecorder.prototype.writeSonogram = function(win, fail, width, height, duration) {

	win = win || function() {};
	fail = fail || function() {};

	width = width || 320;
	height = height || 120;
	duration = duration || 30;

	cordova.exec(generateDelayedCallback(win), fail, "AudioRecorder", "writeSonogram", [width, height, duration]);

}

audioRecorder.prototype.writeRecording = function(win, fail, duration) {

	win = win || function() {};
	fail = fail || function() {};

	duration = duration || 30;

	cordova.exec(generateDelayedCallback(win), fail, "AudioRecorder", "writeRecording", [duration]);

}

