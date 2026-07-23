(function () {
  'use strict';

  const DATA_URL =
    'https://temikeezy.github.io/nigeria-geojson-data/data/lgas-with-wards.json';

  const STATE_ENUMS = {
    'Abia':'ABIA','Adamawa':'ADAMAWA','Akwa Ibom':'AKWA_IBOM','Anambra':'ANAMBRA',
    'Bauchi':'BAUCHI','Bayelsa':'BAYELSA','Benue':'BENUE','Borno':'BORNO',
    'Cross River':'CROSS_RIVER','Delta':'DELTA','Ebonyi':'EBONYI','Edo':'EDO',
    'Ekiti':'EKITI','Enugu':'ENUGU','Gombe':'GOMBE','Imo':'IMO','Jigawa':'JIGAWA',
    'Kaduna':'KADUNA','Kano':'KANO','Katsina':'KATSINA','Kebbi':'KEBBI','Kogi':'KOGI',
    'Kwara':'KWARA','Lagos':'LAGOS','Nasarawa':'NASARAWA','Niger':'NIGER','Ogun':'OGUN',
    'Ondo':'ONDO','Osun':'OSUN','Oyo':'OYO','Plateau':'PLATEAU','Rivers':'RIVERS',
    'Sokoto':'SOKOTO','Taraba':'TARABA','Yobe':'YOBE','Zamfara':'ZAMFARA',
    'Federal Capital Territory':'FEDERAL_CAPITAL_TERRITORY',
    'FCT':'FEDERAL_CAPITAL_TERRITORY'
  };

  const FALLBACK = {
    Lagos: {
      Agege: ['Dopemu','Isale Oja','Oke Koto','Orile Agege'],
      'Ajeromi-Ifelodun': ['Ajegunle','Layeni','Tolu'],
      Alimosho: ['Akowonjo','Egbeda','Idimu','Igando','Ikotun','Ipaja'],
      'Amuwo-Odofin': ['Festac Town','Kirikiri','Mile 2','Satellite Town'],
      Apapa: ['Apapa GRA','Ijora','Olodi Apapa'],
      Badagry: ['Ajara','Badagry Central','Ibereko'],
      Epe: ['Epe Central','Eredo','Itoikin'],
      'Eti-Osa': ['Ajah','Ikoyi','Lekki','Victoria Island'],
      'Ibeju-Lekki': ['Akodo','Awoyaya','Bogije','Eleko'],
      'Ifako-Ijaiye': ['Fagba','Ifako','Iju','Ojokoro'],
      Ikeja: ['Alausa','GRA','Maryland','Opebi'],
      Ikorodu: ['Igbogbo','Ikorodu Central','Imota'],
      Kosofe: ['Ketu','Mile 12','Ojota','Ogudu'],
      'Lagos Island': ['Marina','Obalende','Onikan'],
      'Lagos Mainland': ['Ebute Metta','Makoko','Yaba'],
      Mushin: ['Idi Oro','Ilupeju','Mushin'],
      Ojo: ['Alaba','Iba','Okokomaiko'],
      'Oshodi-Isolo': ['Ajao Estate','Isolo','Mafoluku','Oshodi'],
      Shomolu: ['Bariga','Onipanu','Shomolu'],
      Surulere: ['Aguda','Bode Thomas','Ijesha','Ojuelegba']
    }
  };

  let dataPromise;

  function normalize(value) {
    return String(value || '')
      .trim()
      .replaceAll('-', '_')
      .replaceAll(' ', '_')
      .toUpperCase();
  }

  function enumToLabel(value) {
    return String(value || '')
      .toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  function stateEnum(label) {
    return STATE_ENUMS[label] || normalize(label);
  }

  function findStateLabel(data, value) {
    const wanted = normalize(value);
    return Object.keys(data).find(label =>
      normalize(label) === wanted || stateEnum(label) === wanted
    ) || '';
  }

  async function load() {
    if (!dataPromise) {
      dataPromise = fetch(DATA_URL, { cache: 'force-cache' })
        .then(response => {
          if (!response.ok) throw new Error('Location data could not be loaded');
          return response.json();
        })
        .catch(() => FALLBACK);
    }
    return dataPromise;
  }

  function setOptions(select, options, selected, placeholder, mapper = x => x) {
    if (!select) return;
    const values = [...new Set(options.filter(Boolean))];
    select.innerHTML =
      `<option value="">${placeholder}</option>` +
      values.map(value => {
        const mapped = mapper(value);
        const isSelected =
          normalize(mapped) === normalize(selected) ||
          normalize(value) === normalize(selected);
        return `<option value="${escapeHtml(mapped)}" ${isSelected ? 'selected' : ''}>${escapeHtml(value)}</option>`;
      }).join('');
    select.disabled = values.length === 0;
  }

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>'"]/g, character => ({
      '&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'
    })[character]);
  }

  async function bind(config) {
    const {
      stateSelect,
      lgaSelect,
      areaSelect,
      selectedState = '',
      selectedLga = '',
      selectedArea = '',
      stateValue = 'enum',
      onChange
    } = config;

    if (!stateSelect || !lgaSelect || !areaSelect) return;

    stateSelect.disabled = true;
    lgaSelect.disabled = true;
    areaSelect.disabled = true;

    const data = await load();
    const stateLabels = Object.keys(data).sort((a, b) => a.localeCompare(b));
    const stateMapper = stateValue === 'name' ? value => value : stateEnum;

    setOptions(
      stateSelect,
      stateLabels,
      selectedState,
      'Select state',
      stateMapper
    );
    stateSelect.disabled = false;

    async function populateLgas(preferred = '') {
      const label = findStateLabel(data, stateSelect.value);
      const lgas = label ? Object.keys(data[label] || {}).sort((a,b)=>a.localeCompare(b)) : [];
      setOptions(lgaSelect, lgas, preferred, 'Select LGA');
      lgaSelect.disabled = lgas.length === 0;
      await populateAreas(preferred ? selectedArea : '');
    }

    async function populateAreas(preferred = '') {
      const label = findStateLabel(data, stateSelect.value);
      const lga = lgaSelect.value;
      const entries = label && lga ? (data[label]?.[lga] || []) : [];
      const areas = entries.map(entry =>
        typeof entry === 'string' ? entry : entry.name
      ).sort((a,b)=>a.localeCompare(b));
      setOptions(areaSelect, areas, preferred, 'Select area');
      areaSelect.disabled = areas.length === 0;
      if (typeof onChange === 'function') onChange();
    }

    stateSelect.addEventListener('change', async () => {
      await populateLgas('');
      if (typeof onChange === 'function') onChange();
    });

    lgaSelect.addEventListener('change', async () => {
      await populateAreas('');
      if (typeof onChange === 'function') onChange();
    });

    areaSelect.addEventListener('change', () => {
      if (typeof onChange === 'function') onChange();
    });

    await populateLgas(selectedLga);

    if (selectedLga) {
      lgaSelect.value = selectedLga;
      await populateAreas(selectedArea);
      if (selectedArea) areaSelect.value = selectedArea;
    }
  }

  window.OyukiLocations = {
    bind,
    load,
    stateEnum,
    enumToLabel
  };
})();
