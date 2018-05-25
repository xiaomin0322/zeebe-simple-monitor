var viewer = null;
var container = null;

let restAccess = "api/";
let brokerLogs = {};

let topics = [];

var config;

var workflowDefinitions;
var selectedWorkflowDefinition;

var workflowInstances;
var selectedWorkflowInstance;
var currentPage;

function cleanupData() {
			$.ajax({
		             type : 'POST',
		             url: restAccess + 'broker/cleanup',
		             success: function (result) {
					    loadConfiguration();
					    refresh();		
		             },
		             error: function (xhr, ajaxOptions, thrownError) {
		             	console.log(xhr);
		             	console.log(thrownError);
		             	showError(xhr.responseJSON.message);
		             },
		             crossDomain: true,
		    });				
}




function init(page) {	
	currentPage = page;
	refresh();
}

function refresh() {
	if (currentPage=='broker') {
		loadConfiguration();
	} else if (currentPage=='definition') {
		loadWorkflowDefinitions();		
	} else if (currentPage=="instance") {
		loadWorkflowInstances();		
	} else if (currentPage=="logs") {
		loadBrokerLogs();		
	}
	
	checkConnection();
}

// -------- config page

function loadConfiguration() {
	
	$.ajax({
        type : 'GET',
        url: restAccess + 'broker/',
        contentType: 'application/text; charset=utf-8',
        success: function (cfg) {
       	 config = cfg
       	 renderConfiguration(config)
        },
        error: function (xhr, ajaxOptions, thrownError) {
       	 showErrorResonse(xhr, ajaxOptions, thrownError);
        },
        timeout: 3000,
        crossDomain: true,
	});	
}

function renderConfiguration(config) {
	
	if (config) {
		
		$("#connection-string").html(config.connectionString)
		
		$("#connect-section").hide()
		$("#configuration-section").show()

	} else {
		$("#configuration-section").hide()
		$("#connect-section").show()
	}
}

function connect() {
	connectToBroker( $('#brokerConnection').val() );
	$('#brokerConnection').text('');
}

function connectToBroker(connectionString) {
	$.ajax({
             type : 'POST',
             url: restAccess + 'broker/connect',
             data: connectionString,
             contentType: 'application/text; charset=utf-8',
             success: function (cfg) {
            	loadConfiguration();
            	refresh();		
             },
             error: function (xhr, ajaxOptions, thrownError) {
            	 showErrorResonse(xhr, ajaxOptions, thrownError);
             },
             timeout: 5000,
             crossDomain: true,
    });				
}	

function checkConnection() {
	$.ajax({
	     type : 'GET',
	     url: restAccess + 'broker/check-connection',
	     contentType: 'application/text; charset=utf-8',
	     success: function (result) {
	    	 renderConnectionState(result)	
	    	 
	    	 //setTimeout(checkConnection(), 10000);
	     },
	     error: function (xhr, textStatus, thrownError) {
	    	 	 
	    	 renderConnectionState(false)
	    	 
	    	 // setTimeout(checkConnection(), 30000);
	     },
	     timeout: 3000,
	     crossDomain: true,
	});				
}	

function renderConnectionState(connected) {
	if (connected) {
	$('#connection-state').html('<span class="label label-success">connected</span>');
	}
	else {
	$('#connection-state').html('<span class="label label-warning">disconnected</span>');
	}
}


//-------- workflow page

function loadWorkflowDefinitions() {
	$.get(restAccess + 'workflow/', function(result) {
	    workflowDefinitions = result;
	    if (!selectedWorkflowDefinition && workflowDefinitions && workflowDefinitions.length>0) {
	    	selectedWorkflowDefinition = workflowDefinitions[workflowDefinitions.length - 1];
	    }
	    renderWorkflowDefinitionTable();
	    renderSelectedWorkflowDefinition();
	});			
}

function renderWorkflowDefinitionTable() {
	$("#workflowDefinitionTable > tbody").html("");
	for (index = workflowDefinitions.length-1; index >= 0; --index) {
		var def = workflowDefinitions[index];
		var selectedClass = '';
		if (selectedWorkflowDefinition && def.key==selectedWorkflowDefinition.key && def.version==selectedWorkflowDefinition.version) {
			selectedClass ='class="tngp-table-selected"';
		}
		$('#workflowDefinitionTable tbody').append("<tr><td "+selectedClass+"><a onclick='selectWorkflowDefinition("+index+")'>"+def.bpmnProcessId + "(" + def.version + ")" +"</a></td><td "+selectedClass+">"+def.countRunning+"</td></tr>");
	}
}	

function selectWorkflowDefinition(index) {
	selectedWorkflowDefinition = workflowDefinitions[index];
	
	renderWorkflowDefinitionTable(); // set selected could be done with less overhead - but this is quick for now
	renderSelectedWorkflowDefinition();
}

function renderSelectedWorkflowDefinition() {
	if (selectedWorkflowDefinition) {
		$('#workflowKey').html(selectedWorkflowDefinition.workflowKey);
		$('#bpmnProcessId').html(selectedWorkflowDefinition.bpmnProcessId);
		$('#workflowVersion').text(selectedWorkflowDefinition.version);
		$('#topic').text(selectedWorkflowDefinition.topic);

		$('#countRunning').text(selectedWorkflowDefinition.countRunning);
		$('#countEnded').text(selectedWorkflowDefinition.countEnded);

						viewer.importXML(selectedWorkflowDefinition.resource, function(err) {
							if (err) {
								console.log('error rendering', err);
				             	showError(err);
							} else {
								var canvas = viewer.get('canvas');
								var overlays = viewer.get('overlays');

								container.removeClass('with-error')
										 .addClass('with-diagram');

								// zoom to fit full viewport
								canvas.zoom('fit-viewport');
							}
						});
    }
}

function startWorkflowInstance() {
	console.log(selectedWorkflowDefinition);
	if (selectedWorkflowDefinition) {
		console.log(JSON.stringify( $('#payload').val() ));
		$.ajax({
	             type : 'POST',
	             url: restAccess + 'workflow/' + selectedWorkflowDefinition.workflowKey,
	             data:  $('#payload').val(),
	             contentType: 'application/json; charset=utf-8',
	             success: function (result) {
	             	setTimeout(function() {
    					refresh();
					}, 1000);
	             },
	             error: function (xhr, ajaxOptions, thrownError) {
	            	 showErrorResonse(xhr, ajaxOptions, thrownError);
	             },
            	 timeout: 5000,
	             crossDomain: true,
	    });
	}
}

//-------- instance page

function loadWorkflowInstances() {
	$.get(restAccess + 'instance/', function(result) {
	    workflowInstances = result;
	    if (workflowInstances && workflowInstances.length>0) {
	    	
	      var index = -1
	      if (selectedWorkflowInstance) {
	    	index = workflowInstances.findIndex(function(wf) { return wf.id == selectedWorkflowInstance.id});  
	      }	      
	      if (index < 0) {
	        index = workflowInstances.length - 1
	      }
	      selectedWorkflowInstance = workflowInstances[index]
	    }
	    else {
	      selectedWorkflowInstance = null
	    }	    	

	    renderWorkflowInstanceTable();
	    renderSelectedWorkflowInstance();
	});			
}

function renderWorkflowInstanceTable() {
	$("#workflowInstanceTable > tbody").html("");
	for (index = workflowInstances.length-1; index >= 0; --index) {
		var def = workflowInstances[index];
		var selectedClass = '';
		if (selectedWorkflowInstance && def.id==selectedWorkflowInstance.id) {
			selectedClass ='class="tngp-table-selected"';
		}
		$('#workflowInstanceTable tbody').append(
			"<tr><td "+selectedClass+"><a onclick='selectWorkflowInstance("+index+")'>"+def.workflowInstanceKey +"</a></td><td "+selectedClass+">"+def.bpmnProcessId+"</td></tr>");
	}
}	

function selectWorkflowInstance(index) {
	selectedWorkflowInstance = workflowInstances[index];
	
	renderWorkflowInstanceTable(); // set selected could be done with less overhead - but this is quick for now
	renderSelectedWorkflowInstance();
}

function renderSelectedWorkflowInstance() {
	if (selectedWorkflowInstance) {
		
		$('#workflowInstanceKey').html(selectedWorkflowInstance.workflowInstanceKey);
		if (selectedWorkflowInstance.ended) {
			$('#workflowRunning').html("Ended");
		} else {
			$('#workflowRunning').html("Running");
		}

		$('#workflowKey').html(selectedWorkflowInstance.workflowKey);
		$('#payload').val(
			JSON.stringify(
				JSON.parse(selectedWorkflowInstance.payload), undefined, 2
			));
		
		renderIncidentsTable();

		$('#workflowInstanceInfo').text('');
		$.get(restAccess + 'workflow/' + selectedWorkflowInstance.workflowKey, function(result) {
			viewer.importXML(result.resource, function(err) {
							if (err) {
								console.log('error rendering', err);
				             	showError(err);
							} else {
								var canvas = viewer.get('canvas');
								var overlays = viewer.get('overlays');
								var injector = viewer.get('injector');

								container.removeClass('with-error')
										 .addClass('with-diagram');

								// zoom to fit full viewport
								canvas.zoom('fit-viewport');

								addBpmnOverlays(canvas, overlays, selectedWorkflowInstance);
								markSequenceFlows(injector, selectedWorkflowInstance);
							}
			});
		});
    }
}

function renderIncidentsTable() {
	$("#incidentsTable > tbody").html("");
	for (index = 0; index < selectedWorkflowInstance.incidents.length; ++index) {
		var incident = selectedWorkflowInstance.incidents[index];
		$('#incidentsTable tbody').append("<tr><td>"+incident.errorType+"</td><td>"+incident.errorMessage+"</td></tr>");
	}
}

//-------- log page

function loadBrokerLogs() {
	$.get(restAccess + 'log/', function(logs) {
		brokerLogs = logs;
		renderBrokerLogsTable();
	});
}

function renderBrokerLogsTable() {
	$("#brokerLogsTable > tbody").html("");

			for (index = brokerLogs.length-1; index >= 0; --index) {

				var log = brokerLogs[index];
				var record = log.content
                var json = JSON.parse(record);
				var jsonString = JSON.stringify(json, null, 4)
                
				console.log(jsonString)
				
				$('#brokerLogsTable tbody').append(
					"<tr><td><p style='white-space:pre'>"+jsonString+"</p></td>"
					+"</td></tr>");
			}
}



function showError(errorText) {
	$("#errorText").html(errorText);
	$("#errorPanel").show();
}
function ackError() {
	$("#errorPanel").hide();
}

function showErrorResonse(xhr, ajaxOptions, thrownError) {
	if (xhr.responseJSON) {
		showError(xhr.responseJSON.message);
	}
	else {
		showError(thrownError);
	}	
}


function uploadModels() {
  	
	var fileUpload = $('#documentToUpload').get(0);

	var filesToUpload = {
		broker: $('#selectedTopicDropdown').val(), 
		files: []
	} 

	var processUploadedFile = function(fileUpload, index) {
		return function(e) {
		            var binary = '';
		            var bytes = new Uint8Array( e.target.result );
		            var len = bytes.byteLength;
		            for (var j = 0; j < len; j++) {
		                binary += String.fromCharCode( bytes[ j ] );
		            }

		            var currentFile = {
		            	filename: fileUpload.files[index].name,
		            	mimeType: fileUpload.files[index].type,
		            	content:  btoa(binary)
		            }

		            filesToUpload.files.push(currentFile);

		            // if all files are processed - do the upload
		            if (filesToUpload.files.length == fileUpload.files.length) {
		            	uploadFiles();
		            }
		};
	}

    // read all selected files
	if(typeof FileReader === 'function' && fileUpload.files.length > 0) {
		for (index = 0; index < fileUpload.files.length; ++index) {	  

		    var reader = new FileReader();
		    reader.onloadend = processUploadedFile(fileUpload, index);
            reader.readAsArrayBuffer(fileUpload.files[index]);
        }
    }
	    
	var uploadFiles = function() {
	    $.ajax({
	             type : 'POST',
	             url: restAccess + 'workflow/',
	             data:  JSON.stringify(filesToUpload),
	             contentType: 'application/json; charset=utf-8',
	             success: function (result) {
	             	setTimeout(function() {
	             		selectedWorkflowDefinition = null;
    					refresh();
					}, 1000);
	             },
	             error: function (xhr, ajaxOptions, thrownError) {
	            	 showErrorResonse(xhr, ajaxOptions, thrownError);
	             },
            	 timeout: 5000,
	             crossDomain: true,
	    });
	}; 
      
    
}
	
function addBpmnOverlays(canvas, overlays, workflowInstance) {
	
		for (index = 0; index < workflowInstance.runningActivities.length; ++index) {
	    	canvas.addMarker(workflowInstance.runningActivities[index], 'highlight');
		}
	
        for (index = 0; index < workflowInstance.endedActivities.length; ++index) {
				overlays.add(workflowInstance.endedActivities[index], {
				  position: {
				    top: 0,
				    left: 0
				  },
				  html: '<div class="bpmn-badge"><span class="glyphicon glyphicon-ok"></span></div>'
				});
		}
        
        for (index = 0; index < workflowInstance.incidents.length; ++index) {
			overlays.add(workflowInstance.incidents[index].activityId, {
			  position: {
			    top: 0,
			    left: 0
			  },
			  html: '<div class="bpmn-badge error"><span class="glyphicon glyphicon glyphicon-flash"></span></div>'
			});			
        }
}	

function markSequenceFlows(injector, workflowInstance) {
	
	var elementRegistry = injector.get('elementRegistry'),
		graphicsFactory = injector.get('graphicsFactory');
	
	var takenSequenceFlows = workflowInstance.takenSequenceFlows.map(function(id) {
		return elementRegistry.get(id);
	});
	
	takenSequenceFlows.forEach(function(sequenceFlow) {
		var gfx = elementRegistry.getGraphics(sequenceFlow);
		
		colorSequenceFlow(graphicsFactory, sequenceFlow, gfx, '#52b415');
	});
}

function colorSequenceFlow(graphicsFactory, sequenceFlow, gfx, color) {
	var businessObject = sequenceFlow.businessObject,
		di = businessObject.di;
	
	di.set('stroke', color);
	di.set('fill', color);
	
	graphicsFactory.update('connection', sequenceFlow, gfx);
}

function updatePayload() {
	if (selectedWorkflowInstance) {
		$.ajax({
	             type : 'PUT',
	             url: restAccess + 'instance/' + selectedWorkflowInstance.id + "/update-payload",
	             data:  $('#payload').val(),
	             contentType: 'application/json; charset=utf-8',
	             success: function (result) {
	             	setTimeout(function() {
    					refresh();
					}, 1000);
	             },
	             error: function (xhr, ajaxOptions, thrownError) {
	            	 showErrorResonse(xhr, ajaxOptions, thrownError);
	             },
            	 timeout: 5000,
	             crossDomain: true,
	    });
	}
}

function cancelWorkflowInstance() {
	if (selectedWorkflowInstance) {
		$.ajax({
	             type : 'DELETE',
	             url: restAccess + 'instance/' + selectedWorkflowInstance.id,
	             contentType: 'application/json; charset=utf-8',
	             success: function (result) {
	             	setTimeout(function() {
    					refresh();
					}, 1000);
	             },
	             error: function (xhr, ajaxOptions, thrownError) {
	            	 showErrorResonse(xhr, ajaxOptions, thrownError);
	             },
            	 timeout: 5000,
	             crossDomain: true,
	    });
	}
}
