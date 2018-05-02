 $("#addButton").click(function () {
                        GetXYList(user, exp, tstamp);

                        var inputHtml = $('<div class="panel panel-default">' +
                            '<div class="panel-heading">' +
                                'Gating image' +
                                '<button class="pull-right btn btn-primary btn-xs" id="addButton" style="display:none">Add</button>' +
                            '</div>' +
                            '<div class="panel-body">' +
                                '<div class="form-group">' +
                                    '<label for="x">X</label>' +
                                    '<select name="x" id="x" class="form-control input" placeholder="Choose X" required>' +
                                        '<option value="">Choose X</option>' +
                                    '</select>' +
                                '</div>' +
                                '<div class="form-group">' +
                                    '<label for="y">Y</label>' +
                                    '<select name="y" id="y" class="form-control input" placeholder="Choose Y" required>' +
                                        '<option value="">Choose Y</option>' +
                                    '</select>' +
                                '</div>' +
                                '<div class="form-group" id="imgForGate" style="display: none;">' +
                                    '<img src="/images/cinqueterre.jpg" class="img-rounded" alt="Cinque Terre" width="304" height="236" id="photo">' +
                                '</div>' +
                            '</div>' +
                        '</div>');
                        $("#imagecontainer").append(inputHtml);
                        });


  $("#addButton").click(function () {
                         var count = function(i, val) { return val*1+1 } ;
                         var inputHtml = $('<div class="panel panel-default">' +
                             '<div class="panel-heading">' +
                                 'Gating image' +
                                 '<button class="pull-right btn btn-primary btn-xs" id="addButton" style="display:none">Add</button>' +
                             '</div>' +
                             '<div class="panel-body">' +
                                 '<div class="form-group">' +
                                     '<label for="x' + count+1 + '">X</label>' +
                                     '<select name="x' + count+1 + '" id="x' + count+1 + '" class="form-control input" placeholder="Choose X" required>' +
                                         '<option value="">Choose X</option>' +
                                     '</select>' +
                                 '</div>' +
                                 '<div class="form-group">' +
                                     '<label for="y' + count+1 + '">Y</label>' +
                                     '<select name="y' + count+1 + '" id="y' + count+1 + '" class="form-control input" placeholder="Choose Y" required>' +
                                         '<option value="">Choose Y</option>' +
                                     '</select>' +
                                 '</div>' +
                                 '<div class="form-group" id="imgForGate" style="display: none;">' +
                                     '<img src="/images/cinqueterre.jpg" class="img-rounded" alt="Cinque Terre" width="304" height="236" id="photo">' +
                                 '</div>' +
                             '</div>' +
                         '</div>');
                         $("#imagecontainer").append(inputHtml);
                         GetXYList($("#user").val(), $("#exp").val(), $("#tstamp").val(), count);
                         });