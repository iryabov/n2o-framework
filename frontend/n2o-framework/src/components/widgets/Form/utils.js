import _, {
  isEqual,
  get,
  map,
  reduce,
  set,
  merge,
  every,
  isNil,
  isObject,
  has,
} from 'lodash';

/**
 * Возвращает id первового поля, на котором может быть установлен автофокус
 * @param fields
 * @return {*}
 */
export function getAutoFocusId(fields) {
  for (let field of fields) {
    if (!field.readOnly && field.visible !== false && field.enabled !== false) {
      return field.id;
    }
  }
}

/**
 *
 * Делает из сложного объекта с филдами разных уровнях плоский массив филдов (обходит объект рекурсивно)
 * @param obj - объект, откуда достаем филды
 * @param fields  - текущий массив филдов
 * @example
 * // вернет плоский массив филдов fieldset'а
 * flatFields(fieldset, [])
 */
export function flatFields(obj, fields) {
  fields = [];
  if (_.isObjectLike(obj)) {
    _.each(obj, (v, k) => {
      if (k === 'fields') {
        fields = fields.concat(obj.fields);
      } else {
        fields = fields.concat(flatFields(obj[k], fields));
      }
    });
  }
  return fields;
}

/**
 * Запрашивает данные, если зависимое значение было изменено
 * @param prevState
 * @param state
 * @param ref
 */
export function fetchIfChangeDependencyValue(prevState, state, ref) {
  if (!isEqual(prevState, state) && ref && ref.props._fetchData) {
    const { _fetchData, size, labelFieldId } = ref.props;
    _fetchData({
      size: size,
      [`sorting.${labelFieldId}`]: 'ASC',
    });
  }
}

export const getFieldsKeys = fieldsets => {
  const keys = [];

  const mapFields = fields => {
    map(fields, ({ id }) => keys.push(id));
  };

  const mapCols = cols => {
    map(cols, col => {
      if (has(col, 'cols')) {
        mapCols(col.cols);
      } else if (has(col, 'fields')) {
        mapFields(col.fields);
      } else if (has(col, 'fieldsets')) {
        keys.push(...getFieldsKeys(col.fieldsets));
      }
    });
  };

  map(fieldsets, ({ rows }) =>
    map(rows, row => {
      mapCols(row.cols);
    })
  );

  return keys;
};

const pickByPath = (object, arrayToPath) =>
  reduce(
    arrayToPath,
    (o, p) => {
      if (has(object, p)) {
        return set(o, p, get(object, p));
      }
    },
    {}
  );

export const setWatchDependency = (state, props, dependencyType) => {
  const { dependency, form, modelPrefix } = props;

  const pickByReRender = (acc, { type, on }) => {
    if (on && type === dependencyType) {
      const formOn = map(on, item =>
        ['models', modelPrefix, form, item].join('.')
      );
      return merge(acc, pickByPath(state, formOn));
    }
    return acc;
  };

  return reduce(dependency, pickByReRender, {});
};