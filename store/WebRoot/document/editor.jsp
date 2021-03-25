<!DOCTYPE html>
<html>
    <head>
        <title>${title}</title>
        <script type="text/javascript" src="${apiUrl}"></script>
        <script type="text/javascript" language="javascript">
        var docEditor;
        var innerAlert = function (message) {
            if (console && console.log)
                console.log(message);
        };
        var onAppReady = function () {
            innerAlert("Document editor ready");
        };
        var onDocumentStateChange = function (event) {
            var title = document.title.replace(/\*$/g, "");
            document.title = title + (event.data ? "*" : "");
        };
        var onRequestEditRights = function () {
            location.href = location.href.replace(RegExp("mode=view\&?", "i"), "");
        };
        var onError = function (event) {
            if (event)
                innerAlert(event.data);
        };
        var onOutdatedVersion = function (event) {
            location.reload(true);
        };
        var config = ${configJson};
        config.width = "100%";
        config.height = "100%";
        config.events = {
            "onAppReady": onAppReady,
            "onDocumentStateChange": onDocumentStateChange,
            'onRequestEditRights': onRequestEditRights,
            "onError": onError,
            "onOutdatedVersion": onOutdatedVersion,
        };
        var initEditor = function () {
            docEditor = new DocsAPI.DocEditor("onlyoffice", config);
            fixSize();
        };
        var fixSize = function () {
            var el = document.getElementById("docwrapper");
            el.style.height = screen.availHeight + "px";
            window.scrollTo(0, -1);
            el.style.height = window.innerHeight + "px";
        };
        if (window.addEventListener) {
            window.addEventListener("load", initEditor);
        } else if (window.attachEvent) {
            window.attachEvent("load", initEditor);
        }
        </script>
    </head>
    <body>
        <div id="docwrapper">
            <div id="onlyoffice"></div>
        </div>
    </body>
</html>