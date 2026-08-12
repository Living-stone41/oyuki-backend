(function(){
  'use strict';

  const O = window.Oyuki;
  if (!O) return;

  const $ = (id) => document.getElementById(id);
  const stateSelect = $('state');
  const lgaSelect = $('lga');
  const searchInput = $('marketSearch');
  const marketGrid = $('marketGrid');
  const locationStatus = $('marketLocationStatus');
  const useLocationButton = $('useMyLocation');

  let allMarkets = [];
  let detectedLgaId = null;

  function text(value){
    return O.escapeHtml(value == null ? '' : String(value));
  }

  function renderMarkets(markets){
    const query = (searchInput?.value || '').trim().toLowerCase();
    const rows = (Array.isArray(markets) ? markets : []).filter(m => {
      if (!query) return true;
      const haystack = [m.name,m.address,m.categories,m.lga?.name,m.lga?.state?.name]
        .filter(Boolean).join(' ').toLowerCase();
      return haystack.includes(query);
    });

    if (!rows.length){
      marketGrid.innerHTML = '<div class="col-12"><div class="market-empty"><i class="bi bi-shop"></i><h3>No active markets found</h3><p>Try another LGA or ask an admin to add markets for this area.</p></div></div>';
      return;
    }

    marketGrid.innerHTML = rows.map(m => {
      const lga = m.lga?.name || '';
      const state = m.lga?.state?.name || '';
      return `<div class="col-md-6 col-lg-4"><article class="market-card h-100">
        <div class="market-card-icon"><i class="bi bi-shop-window"></i></div>
        <div class="market-card-body">
          <div class="market-card-kicker">${text(lga || 'Local market')}</div>
          <h3>${text(m.name)}</h3>
          <p class="market-card-location"><i class="bi bi-geo-alt"></i> ${text([m.address,lga,state].filter(Boolean).join(', '))}</p>
          <p class="market-card-categories">${text(m.categories || 'Fresh produce and market goods')}</p>
          <a class="btn btn-brand" href="shop.html?marketId=${encodeURIComponent(m.id)}">Shop this market</a>
        </div>
      </article></div>`;
    }).join('');
  }

  async function loadStates(){
    const states = O.unwrap(await O.Api.get('/market-directory/states', false)) || [];
    stateSelect.innerHTML = '<option value="">Select State</option>' + states.map(s => `<option value="${s.id}">${text(s.name)}</option>`).join('');
    const lagos = states.find(s => String(s.name).toLowerCase() === 'lagos');
    if (lagos){
      stateSelect.value = lagos.id;
      await loadLgas(lagos.id);
    }
  }

  async function loadLgas(stateId){
    lgaSelect.innerHTML = '<option value="">All LGAs</option>';
    if (!stateId) return;
    const lgas = O.unwrap(await O.Api.get(`/market-directory/lgas?stateId=${encodeURIComponent(stateId)}`, false)) || [];
    lgaSelect.innerHTML += lgas.map(l => `<option value="${l.id}">${text(l.name)}</option>`).join('');
  }

  async function loadMarkets(){
    const lgaId = lgaSelect.value;
    const stateId = stateSelect.value;
    let path = '/market-directory/markets';
    if (lgaId) path += `?lgaId=${encodeURIComponent(lgaId)}`;
    else if (stateId) path += `?stateId=${encodeURIComponent(stateId)}`;

    allMarkets = O.unwrap(await O.Api.get(path, false)) || [];
    renderMarkets(allMarkets);
  }

  function locationErrorMessage(error){
    if (!error) return 'We could not detect your location. Select your LGA manually.';
    if (error.code === 1) return 'Location access was denied. Select your State and LGA manually, or use the location button to try again.';
    if (error.code === 2) return 'Your current location could not be determined. Select your LGA manually.';
    if (error.code === 3) return 'Location detection timed out. Select your LGA manually or try again.';
    return 'We could not detect your location. Select your LGA manually.';
  }

  async function resolveLocation(position){
    const {latitude, longitude} = position.coords;
    locationStatus.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Finding markets in your LGA…';
    try{
      const result = O.unwrap(await O.Api.get(`/market-directory/nearby?lat=${encodeURIComponent(latitude)}&lng=${encodeURIComponent(longitude)}`, false));
      if (!result || !result.lgaId){
        locationStatus.innerHTML = `<i class="bi bi-info-circle"></i> ${text(result?.message || 'Select your LGA manually.')}`;
        return;
      }

      detectedLgaId = result.lgaId;
      if (result.stateId){
        stateSelect.value = result.stateId;
        await loadLgas(result.stateId);
      }
      lgaSelect.value = result.lgaId;
      allMarkets = result.markets || [];
      renderMarkets(allMarkets);
      locationStatus.innerHTML = `<i class="bi bi-geo-alt-fill"></i> Detected <strong>${text(result.detectedLga)}</strong>${result.detectedState ? `, ${text(result.detectedState)}` : ''}. ${text(result.message || '')}`;
      sessionStorage.setItem('oyuki_detected_market_area', JSON.stringify({stateId:result.stateId,lgaId:result.lgaId,lga:result.detectedLga,state:result.detectedState,lat:latitude,lng:longitude}));
    }catch(error){
      locationStatus.innerHTML = `<i class="bi bi-exclamation-circle"></i> ${text(error.message || 'Unable to load nearby markets. Select your LGA manually.')}`;
    }
  }

  function requestLocation(){
    if (!navigator.geolocation){
      locationStatus.textContent = 'Your browser does not support location access. Select your State and LGA manually.';
      return;
    }
    locationStatus.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Waiting for location permission…';
    navigator.geolocation.getCurrentPosition(resolveLocation, error => {
      locationStatus.innerHTML = `<i class="bi bi-geo-alt"></i> ${text(locationErrorMessage(error))}`;
    }, {enableHighAccuracy:true, timeout:12000, maximumAge:300000});
  }

  document.addEventListener('DOMContentLoaded', async () => {
    try{
      await loadStates();
      await loadMarkets();
    }catch(error){
      marketGrid.innerHTML = `<div class="col-12"><div class="alert alert-danger">${text(error.message || 'Market directory is temporarily unavailable.')}</div></div>`;
    }

    stateSelect?.addEventListener('change', async () => {
      detectedLgaId = null;
      await loadLgas(stateSelect.value);
      await loadMarkets();
    });

    lgaSelect?.addEventListener('change', async () => {
      detectedLgaId = lgaSelect.value || null;
      await loadMarkets();
    });

    searchInput?.addEventListener('input', () => renderMarkets(allMarkets));
    useLocationButton?.addEventListener('click', requestLocation);

    const saved = sessionStorage.getItem('oyuki_detected_market_area');
    if (saved){
      try{
        const area = JSON.parse(saved);
        if (area.stateId){
          stateSelect.value = area.stateId;
          await loadLgas(area.stateId);
        }
        if (area.lgaId){
          lgaSelect.value = area.lgaId;
          await loadMarkets();
          locationStatus.innerHTML = `<i class="bi bi-geo-alt-fill"></i> Showing markets in <strong>${text(area.lga || 'your detected LGA')}</strong>.`;
          return;
        }
      }catch{}
    }

    requestLocation();
  });
})();
