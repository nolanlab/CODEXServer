function GetExperiments(user) {
                $("#exp").get(0).options.length = 0;
                $.ajax({
                    type: "GET",
                    url: "/getExperimentList?user="+user,
                    contentType: "application/json",
                    dataType: "json",
                    success: function(msg) {
                        $("#exp").get(0).options.length = 0;
                    $.each(msg, function(i,obj)
                    {
                        var div_data="<option value="+obj+">"+obj+"</option>";
                        $(div_data).appendTo('#exp');
                    });

                    }
                });
        }