(function () {
  const initMobileNav = () => {
    const toggle = document.getElementById('mobileNavToggle');
    const overlay = document.getElementById('sidebarOverlay');
    const sidebar = document.getElementById('sidebarMenu');
    if (!toggle || !overlay || !sidebar) return;

    const setOpen = (open, returnFocus = false) => {
      document.body.classList.toggle('sidebar-open', open);
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
      if (returnFocus) {
        toggle.focus();
      }
    };

    toggle.addEventListener('click', () => {
      setOpen(!document.body.classList.contains('sidebar-open'));
    });

    overlay.addEventListener('click', () => setOpen(false, true));

    sidebar.querySelectorAll('a').forEach((link) => {
      link.addEventListener('click', () => setOpen(false));
    });

    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && document.body.classList.contains('sidebar-open')) {
        setOpen(false, true);
      }
    });

    window.addEventListener('resize', () => {
      if (window.innerWidth > 992) {
        setOpen(false);
      }
    });
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initMobileNav);
  } else {
    initMobileNav();
  }
})();
