package de.kolping.cockpit.mapping.android.web.js

object InjectionScripts {
    const val CAPTURE_NETWORK = """
(function(){
  if(window.__kolpingRecorderInstalled) return;
  window.__kolpingRecorderInstalled = true;

  function genId() { return Date.now() + '_' + Math.random().toString(36).substr(2, 9); }
  function post(data) {
    try { RecorderBridge.postMessage(JSON.stringify(data)); } catch(e) {}
  }

  const origFetch = window.fetch;
  window.fetch = async function(input, init) {
    const callId = genId();
    const url = typeof input === 'string' ? input : input.url;
    const method = (init && init.method) || 'GET';
    post({ type: 'NET_REQ', callId: callId, url: url, method: method, body: init && init.body ? String(init.body).substring(0,1000) : null });
    try {
      const response = await origFetch.apply(this, arguments);
      const clone = response.clone();
      clone.text().then(function(body) {
        post({ type: 'NET_RES', callId: callId, status: response.status, contentType: response.headers.get('content-type'), body: body.substring(0,10000) });
      }).catch(function(){});
      return response;
    } catch(e) { throw e; }
  };

  const XHR = XMLHttpRequest.prototype;
  const origOpen = XHR.open;
  const origSend = XHR.send;
  XHR.open = function(method, url) {
    this.__rec_method = method;
    this.__rec_url = url;
    this.__rec_callId = genId();
    return origOpen.apply(this, arguments);
  };
  XHR.send = function(body) {
    const self = this;
    post({ type: 'NET_REQ', callId: self.__rec_callId, url: self.__rec_url, method: self.__rec_method, body: body ? String(body).substring(0,1000) : null });
    self.addEventListener('load', function() {
      post({ type: 'NET_RES', callId: self.__rec_callId, status: self.status, contentType: self.getResponseHeader('content-type'), body: self.responseText.substring(0,10000) });
    });
    return origSend.apply(this, arguments);
  };

  document.addEventListener('click', function(e) {
    var el = e.target;
    if (!el) return;
    var path = [];
    while (el && el.tagName) {
      var tag = el.tagName.toLowerCase();
      if (el.id) { path.unshift(tag + '#' + el.id); break; }
      else { path.unshift(tag); }
      el = el.parentElement;
    }
    var text = e.target.innerText || e.target.textContent || '';
    post({ type: 'CLICK', cssPath: path.join(' > '), text: text.substring(0,100) });
  }, true);
})();
"""
}
