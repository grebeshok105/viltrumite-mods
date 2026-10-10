#!/usr/bin/env python3
"""Apply lang fragments to all 11 locale files (idempotent).

Fragment: JSON {"<key>": {"en_us": "...", "de_de": "...", ...}}. A locale
missing in a fragment entry falls back to en_us. Existing keys are updated in
place; new keys are appended in fragment order.

Run: python3 tools/lang/apply_lang_fragments.py tools/lang/stage4/*.json
"""
import json
import os
import sys

LANG = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../viltrumitecore/src/main/resources/assets/viltrumitecore/lang')


def main(paths):
    entries = {}
    for path in paths:
        with open(path, encoding='utf-8') as f:
            for key, values in json.load(f).items():
                entries.setdefault(key, {}).update(values)
    for name in sorted(os.listdir(LANG)):
        if not name.endswith('.json'):
            continue
        locale = name[:-5]
        path = os.path.join(LANG, name)
        with open(path, encoding='utf-8') as f:
            data = json.load(f)
        for key, values in entries.items():
            data[key] = values.get(locale, values['en_us'])
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write('\n')
    print('applied', len(entries), 'keys')


if __name__ == '__main__':
    main(sys.argv[1:])
