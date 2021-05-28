$(document).ready(function() {

    $("body").addClass( "js-enabled" );

    // =====================================================
    // Back link mimics browser back functionality
    // =====================================================

    // prevent resubmit warning
    if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
        window.history.replaceState(null, null, window.location.href);
    }
    // back click handle, dependent upon presence of referrer & no host change
    $('#back-link[href="#"]').on('click', function(e){
        e.preventDefault();
        window.history.back();
    });

});