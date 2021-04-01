const { resolve, basename } = require('path');
const { readdirSync } = require('fs');
const { loadImage } = require('canvas');

function* getFiles(dir) {
    const dirents = readdirSync(dir, { withFileTypes: true });
    for (const dirent of dirents) {
        const res = resolve(dir, dirent.name);
        if (dirent.isDirectory()) {
            yield* getFiles(res);
        } else {
            yield res;
        }
    }
}

class SpriteManager {
    #sprites = new Map();
    #imageCache = new Map();

    constructor(dir) {
        for (const file of getFiles(dir)) {
            if (file.endsWith('.png')) {
                this.#sprites.set(basename(file).slice(0, basename(file).length - '.png'.length), file);
            }
        }
    }

    getPath(sprite) {
        return this.#sprites.get(sprite);
    }

    /**
     * @param {string} sprite
     * @return {Promise<Image>}
     */
    async getImage(sprite) {
        if (!this.getPath(sprite)) throw new Error('Sprite doesnt exist')
        if (this.#imageCache.has(sprite)) return this.#imageCache.get(sprite);
        this.#imageCache.set(sprite, await loadImage(this.getPath(sprite)));
        return this.#imageCache.get(sprite)
    }
}

const ContentType = { // TODO: auto generated
    item: [{ name: 'copper', color: '#d99d73ff' }, { name: 'lead', color: '#8c7fa9ff' }, { name: 'metaglass', color: '#ebeef5ff' }, { name: 'graphite', color: '#b2c6d2ff' }, { name: 'sand', color: '#f7cba4ff' }, { name: 'coal', color: '#272727ff' }, { name: 'titanium', color: '#8da1e3ff' }, { name: 'thorium', color: '#f9a3c7ff' }, { name: 'scrap', color: '#777777ff' }, { name: 'silicon', color: '#53565cff' }, { name: 'plastanium', color: '#cbd97fff' }, { name: 'phase-fabric', color: '#f4ba6eff' }, { name: 'surge-alloy', color: '#f3e979ff' }, { name: 'spore-pod', color: '#7457ceff' }, { name: 'blast-compound', color: '#ff795eff' }, { name: 'pyratite', color: '#ffaa5fff' }],
    block: 'block',
    mech_UNUSED: 'mech_UNUSED',
    bullet: 'bullet',
    liquid: 'liquid',
    status: 'status',
    unit: 'unit',
    weather: 'weather',
    effect_UNUSED: 'effect_UNUSED',
    sector: 'sector',
    loadout_UNUSED: 'loadout_UNUSED',
    typeid_UNUSED: 'typeid_UNUSED',
    error: 'error',
    planet: 'planet',
    ammo: 'ammo'
}

class ContentManger {
    item

    static getById(type, id) {
        if (typeof type === 'number') {
            type = Object.keys(ContentType)[type]
        }
        return ContentType[type][id]
    }
}

module.exports = { SpriteManager, ContentManger }
