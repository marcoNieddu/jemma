var queueDivName = "hueResetDiv"
var resetButtonId = "resetPhilipsHue"
var stopResetButtonId = "stopResetPhilipsHue"

function resetPhilips() {
	$("#reset-hue-dialog").dialog(
			{
				open : function(evt, ui) {
					// hide close button on Open
					// $(".ui-dialog-titlebar-close", ui).hide();
					$(this).parent().children().children(
							'.ui-dialog-titlebar-close').hide();
					// set dialog content
					$("#resetting-content").hide();
					$("#confirmation-content").show();
				},
				width : 600,
				// resizable: false,
				modal : true,
				buttons : {
					"Start" : function() {
						$("#confirmation-content").hide();
						// init progressbar
						$("#progressbar").progressbar({
							value : 0
						});
						$(this).dialog('option', 'buttons', {
							// Put the stop button
							'Stop' : function() {
								$(this).dialog('close');
								stopResetPhilips();
							}
						});

						$("#resetting-content").show();

						resetPhilipsProcedure();

					},
					"Cancel" : function() {
						$(this).dialog("close");
					}
				}
			});
}

/**
 * Function invoked when the Philips HUE reset procedure is stopped by the user
 * Flushes the functions queue and updates the GUI
 */
function stopResetPhilips() {
	$("#" + queueDivName).clearQueue();
	alert("If your lamp blinked and become red you can now join it to your own network, otherwise try the same procedure again");
	$("#" + queueDivName).html("");
}

/**
 * Function called when "Reset Philips Hue Button" is pressed. Performs requests
 * to change channel and reset Philips lamps All functions are invoked through
 * jquery queue
 */
function resetPhilipsProcedure() {
	var GAL_IEEE;
	var GAL_PANID;

	/**
	 * JQuery queue compatible version of function to get GAL Pan ID
	 */
	$.fn.getGalPanId = function() {
		var that = this;
		/* 0x80:Short PanId */
		this.queue(function() {
			setTimeout(function() {
				$.ajax({
					url : DEFINEPATH.attributeInfoPath + "?id=0x80",
					type : 'GET'
				}).done(function(data) {
					var dataBig = json_parse(data);

					if (dataBig.status.code == 0) {
						GAL_PANID = dataBig.detail.value;
						console.log("GAL_PANID:" + GAL_PANID);
						that.dequeue();
					} else {
						alert("Error getting PAN ID");

					}
				});
			}, 2000);
		});
		return this;
	}

	/**
	 * JQuery queue compatible version of function to get GAL IEEE
	 */
	$.fn.getGalIeee = function() {
		var that = this;
		/* 0x9A:Extended PanId=GAL IEEE */
		this.queue(function() {
			$.ajax({
				url : DEFINEPATH.attributeInfoPath + "?id=0x9A",
				type : 'GET'
			}).done(function(data) {
				var dataBig = json_parse(data);

				if (dataBig.status.code == 0) {
					GAL_IEEE = dataBig.detail.value;
					console.log("GAL_IEEE:" + dataBig.detail.value)
					that.dequeue();
				} else {
					alert("Error getting PAN ID");

				}
			});
		});
		return this;
	}

	/**
	 * JQuery queue compatible version of function to change GAL channel from
	 * advancedControl.html
	 */
	$.fn.changeChannelQ = function(channel) {

		var that = this;
		this.queue(function() {
			setTimeout(function() {
				changeChannel(channel, function() {
					that.dequeue();
				})
			}, 30000);
		});
		return this;
	}

	/**
	 * JQuery queue compatible version of function invoke GAL REST APIs to send
	 * an inter-pan message
	 */
	$.fn.sendPostRequest = function(afterMsecs, asduLength, asdu) {
		var that = this;
		this.queue(function() {
			setTimeout(function() {

				$.ajax(
						{
							url : DEFINEPATH.sendinterPan
									+ "?timeout=00000014&addr=" + GAL_IEEE
									+ "&panid=" + GAL_PANID + "&asdu=" + asdu,
							type : 'GET'
						}).done(function(data) {
					var dataBig = json_parse(data);

					if (dataBig.status.code == 0) {
						console.log(data);
						that.dequeue();
					} else {
						alert("Error sending InterPan Message");

					}
				});

			}, afterMsecs);
		});
		// action the next method in the chain
		return this;

	}
	/**
	 * Updates the div showing the status of the reset procedure
	 */
	$.fn.updateResetDivStatus = function(message) {
		var that = this;
		this.queue(function() {
			$("#" + queueDivName).html("<i>" + message + "</i>");
			that.dequeue();
		});
		return this;
	}

	$.fn.updateProgressBar = function(value) {
		var that = this;
		this.queue(function() {
			$("#progressbar").progressbar({
				value : value
			});
			that.dequeue();
		});
		return this;
	}

	var that = this;
	$.fn.stopResetPhilipsQ = function() {
		var that = this;
		this.queue(function() {
			$("#reset-hue-dialog").dialog('close');
			stopResetPhilips();
			that.dequeue();
		});
		return this;
	}

	// progress bar value
	var pbValue = 0;

	var step = 100 / (16 * 4); // 100% / (16 total channel number * 4
								// steps_per_channel)

	/**
	 * Function that creates the HTTP ajax calls queue to reset Philips HUE on
	 * an defined Channel
	 */
	function createResetQueueOnChannel(channel) {
		$("#" + queueDivName).updateProgressBar(pbValue += step)
		// set the message on the GUI
		.updateResetDivStatus("Resetting on channel " + channel + "...")
		// this changes the channel and updates the GUI accordingly
		.changeChannelQ(channel).updateProgressBar(pbValue += step)
		// ZCL ScanRequest
		.sendPostRequest(10000, 9, '110100CAFECAFE0233').updateProgressBar(
				pbValue += step)
		// ZCL ResetToFactoryNewRequest
		.sendPostRequest(5000, 7, '110307CAFECAFE').updateProgressBar(
				pbValue += step);
	}

	// get GAL Pan ID and IEEE address first
	$("#" + queueDivName).getGalPanId().getGalIeee();

	$.each([ 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 ],
			function(i, num) {
				createResetQueueOnChannel(num);
			});
	$("#" + queueDivName).stopResetPhilipsQ();

}
