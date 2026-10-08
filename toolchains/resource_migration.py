"""Deterministic top-level vanilla recipe migration, never custom gunpack bytes."""
import json


def migrate_resource_bytes(name, data):
    if name in ('data/tacz/tags/entity_types/interact_key/whitelist.json',
                'data/tacz/tags/entity_type/interact_key/whitelist.json'):
        tag = json.loads(data)
        # Exact target Fabric c:boats includes vanilla chest variants except the
        # newly added poplar chest boat; retain it explicitly until upstream adds it.
        replacements = {'minecraft:boat': '#c:boats', 'minecraft:chest_boat': 'minecraft:poplar_chest_boat'}
        values = [replacements.get(value, value) if isinstance(value, str) else value for value in tag['values']]
        if values != tag['values']:
            tag['values'] = values
            return (json.dumps(tag, indent=2, ensure_ascii=False) + '\n').encode()
        return data
    return migrate_recipe_bytes(name, data)


def migrate_recipe_bytes(name, data):
    parts = name.split('/')
    if len(parts) < 4 or parts[0] != 'data' or parts[2] not in ('recipes', 'recipe') or not name.endswith('.json'):
        return data
    recipe = json.loads(data)
    kind = recipe.get('type')
    if kind not in ('minecraft:crafting_shaped', 'minecraft:crafting_shapeless'):
        return data

    def ingredient(value):
        if isinstance(value, list):
            return [ingredient(entry) for entry in value]
        if isinstance(value, dict) and set(value) == {'item'}:
            return value['item']
        if isinstance(value, dict) and set(value) == {'tag'}:
            tag = value['tag']
            if tag == 'forge:glass':
                tag = 'c:glass_blocks'
            elif tag.startswith('forge:'):
                tag = 'c:' + tag.removeprefix('forge:')
            return '#' + tag
        return value

    if kind == 'minecraft:crafting_shaped':
        recipe['key'] = {key: ingredient(value) for key, value in recipe['key'].items()}
    else:
        recipe['ingredients'] = [ingredient(value) for value in recipe['ingredients']]
    result = recipe['result']
    if isinstance(result, dict) and 'item' in result:
        if 'id' in result:
            raise ValueError('Ambiguous legacy/new result ID: ' + name)
        result['id'] = result.pop('item')
    if isinstance(result, dict) and 'nbt' in result:
        nbt = result.pop('nbt')
        if not isinstance(nbt, dict):
            raise ValueError('Legacy result NBT needs explicit reviewed conversion: ' + name)
        components = result.setdefault('components', {})
        if 'minecraft:custom_data' in components:
            raise ValueError('Ambiguous legacy/new custom data: ' + name)
        components['minecraft:custom_data'] = nbt
    return (json.dumps(recipe, indent=2, ensure_ascii=False) + '\n').encode()
