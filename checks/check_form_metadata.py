"""Check all editable forms against Java declarations, handlers, assets and theme."""
from pathlib import Path
import re
import xml.etree.ElementTree as E

ROOT = Path(__file__).resolve().parents[1]
forms = sorted((ROOT/'src').rglob('*.form'))
component_count = event_count = 0
for form in forms:
    xml = E.parse(form)
    source = form.with_suffix('.java').read_text(encoding='utf-8')
    fields = dict((name, cls.split('<')[0]) for cls,name in re.findall(r'private\s+([\w.<>]+)\s+(\w+)\s*;', source))
    nodes = [n for n in xml.iter() if n.tag in ('Component','Container')]
    for n in nodes:
        assert fields.get(n.get('name')) == n.get('class'), (form.name, n.get('name'), 'Field type mismatch')
    for event in xml.iter('EventHandler'):
        assert re.search(r'void\s+'+event.get('handler')+r'\s*\(', source), (form.name,event.get('handler'))
        event_count += 1
    for image in xml.iter('Image'):
        assert (ROOT/'src'/image.get('name').lstrip('/')).is_file(), (form.name,image.get('name'))
    main = next(n for n in nodes if n.get('name')=='mainPanel')
    preferred = next(p for p in main.find('Properties') if p.get('name')=='preferredSize')
    assert preferred.find('Dimension').get('value')=='[1280, 760]', form.name
    for color in xml.iter('Color'):
        if color.get('type')=='rgb':
            rgb = tuple(int(color.get(k,'0'),16) for k in ('red','green','blue'))
            assert rgb not in ((37,99,235),(30,64,175),(219,234,254),(239,246,255),(191,219,254)), (form.name,rgb)
    assert '/laundry/icons/' not in source, form.name
    component_count += len(nodes)
assert len(forms)==13, len(forms)
assert not (ROOT/'src/laundry/DashboardForm.java').exists(), 'Duplicate old Home remains'
print(f'PASS: {len(forms)} forms, {component_count} matching component declarations, {event_count} event bindings, all image paths, shared canvas and green palette.')
