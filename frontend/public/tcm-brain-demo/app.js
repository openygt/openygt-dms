// TCM Brain - App Logic

document.addEventListener('DOMContentLoaded', () => {
  // Navigation
  const navItems = document.querySelectorAll('.nav-item');
  const pages = document.querySelectorAll('.page');
  const breadcrumb = document.getElementById('breadcrumb');

  const pageNames = {
    diagnosis: '学习大脑 / 诊断',
    herbs: '学习大脑 / 方药',
    acupuncture: '学习大脑 / 针灸',
    symptoms: '学习大脑 / 病症'
  };

  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      const pageId = item.dataset.page;

      // Update nav active state
      navItems.forEach(n => n.classList.remove('active'));
      item.classList.add('active');

      // Update page visibility
      pages.forEach(p => p.classList.remove('active'));
      document.getElementById(`page-${pageId}`).classList.add('active');

      // Update breadcrumb
      breadcrumb.textContent = pageNames[pageId] || '学习大脑';
    });
  });

  // Meridian tag selection
  const meridianTags = document.querySelectorAll('.meridian-tag');
  meridianTags.forEach(tag => {
    tag.addEventListener('click', () => {
      meridianTags.forEach(t => t.classList.remove('active'));
      tag.classList.add('active');
      showToast(`已选择：${tag.textContent}`);
    });
  });

  // Herb tag selection
  const herbTags = document.querySelectorAll('.herb-tag');
  herbTags.forEach(tag => {
    tag.addEventListener('click', () => {
      tag.classList.toggle('active');
    });
  });

  // Checkbox items
  const checkboxItems = document.querySelectorAll('.checkbox-item');
  checkboxItems.forEach(item => {
    const input = item.querySelector('input');
    if (input) {
      input.addEventListener('change', () => {
        item.classList.toggle('checked', input.checked);
      });
    }
  });

  // Search box
  const searchInput = document.querySelector('.search-input');
  if (searchInput) {
    searchInput.addEventListener('keypress', (e) => {
      if (e.key === 'Enter') {
        showToast(`搜索：${searchInput.value}`);
      }
    });
  }

  const searchBtn = document.querySelector('.search-btn');
  if (searchBtn) {
    searchBtn.addEventListener('click', () => {
      if (searchInput && searchInput.value) {
        showToast(`搜索：${searchInput.value}`);
      }
    });
  }
});

// Toast notification
function showToast(message) {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 2500);
}
